/*******************************************************************************
*@Description: ready datas for scmfile meta properties
*@Modify list:
*   2018-6-1 huangxiaoni
*******************************************************************************/
// common connect parameter
if ( typeof(COORDHOSTNAME) == "undefined" ) { COORDHOSTNAME = "localhost"; }
if ( typeof(COORDSVCNAME) == "undefined" ) { COORDSVCNAME = 11810; }
if ( typeof(USERNAME) == "undefined" ) { USERNAME = "sdbadmin"; }
if ( typeof(PASSWORD) == "undefined" ) { PASSWORD = 'sequoiadb'; }
// common system cs/cl
var SYSTEMCS = "SCMSYSTEM";
var WSCL = "WORKSPACE";
// file meta cs/cl
var METACS = "_META";
var CLASS_DEFINE = "CLASS_DEFINE";
var CLASS_ATTR_DEFINE = "CLASS_ATTR_DEFINE";
var CLASS_ATTR_REL = "CLASS_ATTR_REL";

// prepare datas for CLASS_DEFINE
var classDefineDatas = 
	[ {id: "test_class_id_all_001",  name: "class_name_001", create_time: NumberLong("1527674572981"), create_user: "admin"}, 
	  {id: "test_class_id_int_002",  name: "class_name_002", create_time: NumberLong("1527674572981"), create_user: "admin"}, 
	  {id: "test_class_id_str_003",  name: "class_name_003", create_time: NumberLong("1527674572981"), create_user: "admin"}, 
	  {id: "test_class_id_date_004", name: "class_name_004", create_time: NumberLong("1527674572981"), create_user: "admin"}, 
	  {id: "test_class_id_bool_005", name: "class_name_005", create_time: NumberLong("1527674572981"), create_user: "admin"}, 
	  {id: "test_class_id_double_006", name: "class_name_006", create_time: NumberLong("1527674572981"), create_user: "admin"}, 
	  {id: "test_class_id_nothing_007", name: "class_name_006", create_time: NumberLong("1527674572981"), create_user: "admin"}
	];
	
// prepare datas for CLASS_ATTR_DEFINE
var classAttrDefineDatas = [];

var allTypeDatas = // contain all types
	[ {name: "test_attr_name_int_000", desc: "中文", type: "INTEGER", required: false}, 
	  {name: "test_attr_name_string_000", desc: "test", type: "STRING", required: false}, 
	  {name: "test_attr_name_date_000", desc: "test", type: "DATE", required: false}, 
	  {name: "test_attr_name_bool_000", desc: "test", type: "BOOLEAN", required: false}, 
	  {name: "test_attr_name_double_000", desc: "test", type: "DOUBLE", required: false}, 
	  {name: "test_attr_name_string..000", desc: "test", type: "STRING", required: false} ]
classAttrDefineDatas.push( allTypeDatas );

var intDatas = // type is INTEGER
	[   
	  {name: "test_attr_name_int_001", desc: "test", type: "INTEGER", required: true}, 
	  {name: "test_attr_name_int_002", desc: "test", type: "INTEGER", required: false}, 
	  {name: "test_attr_name_int_003", desc: "test", type: "INTEGER", check_rule: {"min": "-2147483648", "max": "2147483647"}, required: true}, 
	  {name: "test_attr_name_int_004", desc: "test", type: "INTEGER", check_rule: {"min": "0", "max": "100"}, required: true} ]
classAttrDefineDatas.push( intDatas );

var strDatas = // type is STRING
	[ {name: "test_attr_name_str_001", desc: "test", type: "STRING", required: true}, 
	  {name: "test_attr_name_str_002", desc: "test", type: "STRING", required: false},  
	  {name: "test_attr_name_str_003", desc: "test", type: "STRING", check_rule: {"maxLength": "1"}, required: true},   
	  {name: "test_attr_name_str_004", desc: "test", type: "STRING", check_rule: {"maxLength": "10"}, required: true},
	  {name: "test_attr_name_str_005", desc: "test", type: "STRING", check_rule: {"maxLength": "2147483647"}, required: true} ]
classAttrDefineDatas.push( strDatas );

var dateDatas = // type is DATE
	[ {name: "test_attr_name_date_001", desc: "test", type: "DATE", required: true}, 
	  {name: "test_attr_name_date_002", desc: "test", type: "DATE", required: false}, 
	  {name: "test_attr_name_date_003", desc: "test", type: "DATE", check_rule: "yyyy-MM-dd-h24:mm:ss.SSS", required: false} ]
classAttrDefineDatas.push( dateDatas );

var boolDatas = // type is BOOLEAN
	[ {name: "test_attr_name_bool_001", desc: "test", type: "BOOLEAN", required: true}, 
	  {name: "test_attr_name_bool_002", desc: "test", type: "BOOLEAN", required: false}, 
	  {name: "test_attr_name_bool_003", desc: "test", type: "BOOLEAN", check_rule: "test", required: false} ]
classAttrDefineDatas.push( boolDatas );

var doubleDatas = // type is DOUBLE
	[ {name: "test_attr_name_double_001", desc: "test", type: "DOUBLE", required: true}, 
	  {name: "test_attr_name_double_002", desc: "test", type: "DOUBLE", required: false},  
	  {name: "test_attr_name_double_003", desc: "test", type: "DOUBLE", check_rule: {"min": "4.9E-324", "max": "1.7976931348623157E308"}, required: true},  
	  {name: "test_attr_name_double_004", desc: "test", type: "DOUBLE", check_rule: {"min": "0.1", "max": "100.0"}, required: true} ]
classAttrDefineDatas.push( doubleDatas );

var noRelDatas = // not rel class
	[ {name: "test_attr_name_notRelClass_001", desc: "test", type: "STRING", required: true} ]
classAttrDefineDatas.push( noRelDatas );


// common connect sdb
try
{
	var db = new Sdb( COORDHOSTNAME, COORDSVCNAME, USERNAME, PASSWORD );
}
catch ( e )
{
   println("failed to connect sdb, connect info["+ COORDHOSTNAME +", "+ COORDSVCNAME +", "
						+ USERNAME +", "+ PASSWORD +"]");
	throw e;
}

main() ;

function main()
{  
	try {
		// get all ws
		println("\n---Begin to get all ws.");
		var wss = getWss( db );
		
		// prepare data
		println("\n---Begin to prepare data for each ws["+ wss +"].");		
		for (var i = 0; i < wss.length; i++) {
			var wsName = wss[i];
			var csName = wsName + METACS;
			
			// for CLASS_DEFINE
			println("\n   Begin to prepare data, cl["+ csName +"."+ CLASS_DEFINE +"].");
			var clDB = db.getCS( csName ).getCL( CLASS_DEFINE );
			clDB.insert( classDefineDatas );
			
			// for CLASS_ATTR_DEFINE
			println("   Begin to prepare data, cl["+ csName +"."+ CLASS_ATTR_DEFINE +"].");	
			var clDB = db.getCS( csName ).getCL( CLASS_ATTR_DEFINE );
			for (var j = 0; j < classAttrDefineDatas.length; j++) {
				clDB.insert( classAttrDefineDatas[j] );
			}
			
			// for CLASS_ATTR_REL
			println("   Begin to prepare data, cl["+ csName +"."+ CLASS_ATTR_REL +"].");
			var clDB = db.getCS( csName ).getCL( CLASS_ATTR_REL );
			insertDataForClassAttrRel( clDB );
		}
	} catch ( e ) {
		throw e;
	}
	println("\n---End.");
}

/* *****************************************************************************
@discription: get all ws
@return: wss
***************************************************************************** */
function getWss() {
	var clDB = db.getCS( SYSTEMCS ).getCL( WSCL );
   var wss = [];
	var cursor = clDB.find();
	while ( info = cursor.next() ) {
		var wsName = info.toObj()["name"];
		wss.push( wsName );
	}
	return wss;
}

/* *****************************************************************************
@discription: insert datas for CLASS_ATTR_REL
***************************************************************************** */
function insertDataForClassAttrRel( clDB ) {
	// for test_class_id_all_001, rel all types
	var relAllDatas = [];
	var class_id_val = classDefineDatas[0]["id"];
	for (var i = 0; i < allTypeDatas.length; i++) {
		var attr_name_val = allTypeDatas[i]["name"];
		var record = {class_id: class_id_val, attr_name: attr_name_val};
		relAllDatas.push( record );
	}
	clDB.insert( relAllDatas );
	
	// for test_class_id_int_002, rel int type data
	var relIntDatas = [];
	var class_id_val = classDefineDatas[1]["id"];
	for (var i = 0; i < intDatas.length; i++) {
		var attr_name_val = intDatas[i]["name"];
		var record = {class_id: class_id_val, attr_name: attr_name_val};
		relIntDatas.push( record );
	}
	clDB.insert( relIntDatas );
	
	// for test_class_id_str_003, rel str type data
	var relStrDatas = [];
	var class_id_val = classDefineDatas[2]["id"];
	for (var i = 0; i < strDatas.length; i++) {
		var attr_name_val = strDatas[i]["name"];
		var record = {class_id: class_id_val, attr_name: attr_name_val};
		relStrDatas.push( record );
	}
	clDB.insert( relStrDatas );
	
	// for test_class_id_date_004, rel date type data
	var relDateDatas = [];
	var class_id_val = classDefineDatas[3]["id"];
	for (var i = 0; i < dateDatas.length; i++) {
		var attr_name_val = dateDatas[i]["name"];
		var record = {class_id: class_id_val, attr_name: attr_name_val};
		relDateDatas.push( record );
	}
	clDB.insert( relDateDatas );
	
	// for test_class_id_bool_005, rel bool type data
	var relBoolDatas = [];
	var class_id_val = classDefineDatas[4]["id"];
	for (var i = 0; i < boolDatas.length; i++) {
		var attr_name_val = boolDatas[i]["name"];
		var record = {class_id: class_id_val, attr_name: attr_name_val};
		relBoolDatas.push( record );
	}
	clDB.insert( relBoolDatas );
	
	// for test_class_id_double_006, rel double type data
	var relDoubleDatas = [];
	var class_id_val = classDefineDatas[5]["id"];
	for (var i = 0; i < doubleDatas.length; i++) {
		var attr_name_val = doubleDatas[i]["name"];
		var record = {class_id: class_id_val, attr_name: attr_name_val};
		relDoubleDatas.push( record );
	}
	clDB.insert( relDoubleDatas );
}

