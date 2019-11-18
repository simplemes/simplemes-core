<script>
  <#assign panel = "${params._panel}"/>
  <#assign variable = "${params._variable}"/>
  ${variable}.xyz = '123';
  ${variable}.abc = function () {
    console.log('abc() called')
  };
  //console.log('${params._panel}');

  ${variable}.display = {
    view: 'form', type: 'clean', margin: 0, padding: 10,
    cols: [
      {width: tk.pw('5%')},
      {
        view: "datatable",
        id: 'theTable',
        //autoConfig:true,
        //height: 150,

        //css: "webix_header_border",
        css: "webix_header_border webix_data_border data-table-with-border",
        headerRowHeight: tk.pw('2em'),
        resizeColumn: {size: 6, headerOnly: true},
        dragColumn: true,
        width: tk.pw('40%'),
        editable: false,
        select: 'row',
        //autowidth: true,
        gravity: 6,
        columns: [
          {id: "order", editor: "text", header: {text: "Order"}, sort: 'text'},
          {id: "lsn", editor: "text", header: {text: "LSN"}},
          {id: "qtyInQueue", editor: "text", header: {text: "In Queue"}, css: {'text-align': 'center'}},
          {id: "qtyInWork", editor: "text", header: {text: "In Work"}, css: {'text-align': 'center'}}
        ],
        data: [
          {order: 'M1001', lsn: "", qtyInQueue: 1.0, qtyInWork: '0'},
          {order: 'M1002', lsn: "", qtyInQueue: 1.2, qtyInWork: '0'},
          {order: 'M1003', lsn: "", qtyInQueue: 1.0, qtyInWork: '0'},
          {order: 'M1004', lsn: "", qtyInQueue: 4.0, qtyInWork: '0'},
          {order: 'M1005', lsn: "", qtyInQueue: 0, qtyInWork: '1.0'},
          {order: 'M1006', lsn: "", qtyInQueue: 1.0, qtyInWork: '0'},
          {order: 'M1007', lsn: "", qtyInQueue: 1.0, qtyInWork: '0'},
          {order: 'M1008', lsn: "SN1000", qtyInQueue: 1.0, qtyInWork: '0'},
          {order: 'M1009', lsn: "SN1001", qtyInQueue: 1.0, qtyInWork: '0'}
        ]
      },
      {width: tk.pw('5%')}
    ]
  };
</script>
