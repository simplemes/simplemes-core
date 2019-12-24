package org.simplemes.eframe.domain.annotation;

import groovy.lang.Closure;
import io.micronaut.core.util.StringUtils;
import io.micronaut.data.repository.GenericRepository;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SimpleMessage;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.simplemes.eframe.ast.ASTUtils;

import javax.persistence.OneToMany;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;


/**
 * This class provides the compile-time AST transformation that adds an extensible field to the given
 * domain class.  This injects the custom field holder as a persistent field with a given maxSize.  Also
 * injects a static (non-persistent) field that holds the custom field definitions for the runtime validation.
 */
@SuppressWarnings({"DefaultAnnotationParam"})
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class DomainEntityTransformation implements ASTTransformation {

  /*
   * The default field name for the static definition holder variable in the domain class.  Not configurable.
   */
  //public static final String DEFAULT_DEFINITION_HOLDER_FIELD_NAME = "_customFieldDef";

  /**
   * Basic Constructor.
   */
  @SuppressWarnings("unused")
  public DomainEntityTransformation() {
  }

  public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
    //System.out.println("visit");
    if (sourceUnit.getAST() == null) {
      return;
    }
    ClassNode ourAnnotationNode = new ClassNode(DomainEntity.class);
    for (ASTNode astNode : astNodes) {
      if (astNode instanceof ClassNode) {
        //System.out.println("Checking astNode2 = " + astNode);
        ClassNode classNode = (ClassNode) astNode;
        List<AnnotationNode> annotations = classNode.getAnnotations(ourAnnotationNode);
        if (annotations.size() > 0) {
          //System.out.println("found");
          transformClass(classNode, sourceUnit);
        }
      }
    }
  }

  /**
   * Transform the given domain class with added features for the @DomainEntity annotation.
   *
   * @param classNode  The class to transform.
   * @param sourceUnit The source the class came from.
   */
  private void transformClass(ClassNode classNode, SourceUnit sourceUnit) {
    classNode.addInterface(new ClassNode(DomainEntityInterface.class));
    addRepositoryField(classNode, sourceUnit);
    addDelegatedMethod("save", "save", false, null, null, classNode, sourceUnit);
    addDelegatedMethod("delete", "delete", false, null, null, classNode, sourceUnit);

    Parameter[] withTransactionParameters = {new Parameter(new ClassNode(Closure.class), "closure")};
    List<Expression> withTxnArgs = new ArrayList<>();
    withTxnArgs.add(new VariableExpression("closure"));
    addDelegatedMethod("withTransaction", "executeWriteClosure", true, withTransactionParameters, withTxnArgs, classNode, sourceUnit);

    Parameter[] parameters = {
        new Parameter(new ClassNode(String.class), "name"),
        new Parameter(new ClassNode(Object.class), "args")};
    List<Expression> mmArgs = new ArrayList<>();
    mmArgs.add(new VariableExpression("name"));
    mmArgs.add(new VariableExpression("args"));
    addDelegatedMethod("$static_methodMissing", "staticMethodMissingHandler", true, parameters, mmArgs, classNode, sourceUnit);

    addLazyChildLoaders(classNode, sourceUnit);
  }

  /**
   * Adds the lazy loader methods for the child lists.
   *
   * @param classNode  The class to transform.
   * @param sourceUnit The source the class came from.
   */
  private void addLazyChildLoaders(ClassNode classNode, SourceUnit sourceUnit) {
    // Find all fields with a @OneToMany annotation.
    for (FieldNode fieldNode : classNode.getFields()) {
      for (AnnotationNode annotationNode : fieldNode.getAnnotations(new ClassNode(OneToMany.class))) {
        //System.out.println(fieldNode.getName()+" ann:" + annotationNode+" "+annotationNode.getClassNode()+" mappedBy:"+annotationNode.getMembers());
        String mappedByFieldName = annotationNode.getMember("mappedBy").getText();
        ClassNode childDomainClassNode = fieldNode.getType();
        GenericsType[] genericsTypes = childDomainClassNode.getGenericsTypes();
        if (genericsTypes == null || genericsTypes.length == 0) {
          String fqName = classNode.getName();
          String s = "Child field " + fieldNode.getName() + " must be have a parameterized type (e.g. List<XYZ>) in " + fqName;
          sourceUnit.getErrorCollector().addError(new SimpleMessage(s, sourceUnit));
          return;
        }
        ClassNode childDomainTypeNode = genericsTypes[0].getType();
        String getterName = "get" + StringUtils.capitalize(fieldNode.getName());
        //  public List lazyChildLoad(Object object,String fieldName, String mappedByFieldName, Class childDomainClazz)
        List<Expression> delegateArgs = new ArrayList<>();
        delegateArgs.add(new ConstantExpression(fieldNode.getName()));
        delegateArgs.add(new ConstantExpression(mappedByFieldName));
        delegateArgs.add(new ClassExpression(childDomainTypeNode));
        addDelegatedMethod(getterName, "lazyChildLoad", false, null, delegateArgs, classNode, sourceUnit);
      }

    }
  }

  /**
   * Adds the static repository field holder to the domain class.
   *
   * @param node       The node for the class itself.
   * @param sourceUnit The source unit being processed.
   */
  private void addRepositoryField(ClassNode node, SourceUnit sourceUnit) {
    // Check the annotation to make sure we have the field the developer intended.
    String fieldName = DomainEntity.DEFAULT_REPOSITORY_FIELD_NAME;

    List list = ASTUtils.addField(fieldName, GenericRepository.class, Modifier.STATIC, 0, false, null, node, sourceUnit);
    addRepositoryGetter((FieldNode) list.get(0), node, sourceUnit);
  }


  /**
   * Adds a getter that finds the correct bean for the repository.  Caches this in the class static variable.
   *
   * @param fieldNode  The node of the field.
   * @param node       The AST Class Node to add this field to.
   * @param sourceUnit The compiler source unit being processed (used to errors).
   */
  private void addRepositoryGetter(FieldNode fieldNode, ClassNode node, SourceUnit sourceUnit) {
    String getterName = "get" + StringUtils.capitalize(fieldNode.getName());
    // Make sure the method doesn't exist already
    if (ASTUtils.methodExists(node, getterName, Parameter.EMPTY_ARRAY)) {
      sourceUnit.getErrorCollector().addError(new SimpleMessage(getterName + "() already exists in " + node, sourceUnit));
      return;
    }
    //      if (repository!=null) {
    VariableExpression variableExpression = new VariableExpression(fieldNode);
    BinaryExpression binaryExpression = new BinaryExpression(variableExpression,
        Token.newSymbol(Types.COMPARE_EQUAL, -1, -1),
        ConstantExpression.NULL);
    BooleanExpression booleanExpression = new BooleanExpression(binaryExpression);

    // repository = DomainEntityHelper.instance.determineRepository(DomainEntity)
    BlockStatement ifBlockStatement = new BlockStatement();
    List<Expression> argumentList = new ArrayList<>();
    argumentList.add(new ClassExpression(node));
    //argumentList.add(new VariableExpression("this"));
    MethodCallExpression method = new MethodCallExpression(
        new PropertyExpression(new ClassExpression(new ClassNode(DomainEntityHelper.class)), "instance"),
        "determineRepository",
        new ArgumentListExpression(argumentList));
    BinaryExpression ifTrueExpression = new BinaryExpression(variableExpression,
        Token.newSymbol(Types.EQUAL, -1, -1),
        method);
    ifBlockStatement.addStatement(new ExpressionStatement(ifTrueExpression));

    BlockStatement blockStatement = new BlockStatement();
    blockStatement.addStatement(new IfStatement(booleanExpression, ifBlockStatement, EmptyStatement.INSTANCE));
    blockStatement.addStatement(new ReturnStatement(new FieldExpression(fieldNode)));

    ClassNode type = new ClassNode(GenericRepository.class);
    MethodNode methodNode = new MethodNode(getterName,
        Modifier.PUBLIC | Modifier.STATIC,
        type,
        Parameter.EMPTY_ARRAY,
        null,
        blockStatement);
    //System.out.println("methodNode" + methodNode);
    node.addMethod(methodNode);

  }

  /**
   * Adds the a method to the class, delegating the actual work to the DomainEntityHelper class.
   * Currently only supports zero arguments.
   *
   * @param methodName        The name of the method to add and the name in the delegated class.
   * @param helperMethodName  The name of the helper method to delegate the call to.
   * @param isStatic          If true, then the method is created as a static method.
   * @param args              The args for the method added to the current class (can be null).
   * @param delegateArguments The arguments passed to the delegate method (this is added as first argument).
   * @param node              The AST Class Node to add this field to.
   * @param sourceUnit        The compiler source unit being processed (used to errors).
   */
  private void addDelegatedMethod(String methodName, String helperMethodName, boolean isStatic,
                                  Parameter[] args, List<Expression> delegateArguments,
                                  ClassNode node, SourceUnit sourceUnit) {
    //System.out.println("methodName:" + methodName);
    // Make sure the method doesn't exist already
    if (ASTUtils.methodExists(node, methodName, Parameter.EMPTY_ARRAY)) {
      sourceUnit.getErrorCollector().addError(new SimpleMessage(methodName + "() already exists in " + node, sourceUnit));
    }

    // return = DomainEntityHelper.instance.XYZ(Object,delegateArgs)

    // Build the argument list for the call to the delegate.  We add this as the first argument.
    List<Expression> argumentList = new ArrayList<>();
    argumentList.add(new VariableExpression("this"));
    if (delegateArguments != null) {
      argumentList.addAll(delegateArguments);
    }
    MethodCallExpression method = new MethodCallExpression(
        new PropertyExpression(new ClassExpression(new ClassNode(DomainEntityHelper.class)), "instance"),
        helperMethodName,
        new ArgumentListExpression(argumentList));

    ReturnStatement returnStatement = new ReturnStatement(method);
    BlockStatement blockStatement = new BlockStatement();
    blockStatement.addStatement(returnStatement);
    int modifier = Modifier.PUBLIC;
    if (isStatic) {
      modifier = modifier | Modifier.STATIC;
    }

    if (args == null) {
      args = Parameter.EMPTY_ARRAY;
    }
    MethodNode methodNode = new MethodNode(methodName,
        modifier,
        new ClassNode(Object.class),
        args,
        null,
        blockStatement);
    node.addMethod(methodNode);

  }


}
