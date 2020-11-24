./gradlew dockerBuild

docker tag mes:latest registry.heroku.com/simplemes-demo/web
docker push registry.heroku.com/simplemes-demo/web
heroku container:release web -a simplemes-demo
