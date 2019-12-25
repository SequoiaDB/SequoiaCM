<?php

include_once "parseConf.php" ;
include_once "function.php" ;

//doxygen���ɵ��ļ���toc.json��ӳ���ϵ
$apiList = array(
   "java"      => 1501487716,
) ;

$root = dirname( __FILE__ ).( getOSInfo() == 'linux' ? "/../.." : "\\..\\.." ) ;

$path = getOSInfo() == 'linux' ? "$root/config/toc.json" : "$root\\config\\toc.json" ;

$editionPath = getOSInfo() == 'linux' ? "$root/config/version.json" : "$root\\config\\version.json" ;

$edition = getVersion( $editionPath ) ;
if( $edition == FALSE )
{
   printLog( "Failed to parse config/version.json" ) ;
   exit( 1 ) ;
}

$major = $edition['major'] ;
$minor = $edition['minor'] ;

//������ļ���
$outputFileName = "SequoiaCM_usermanuals_v$major.$minor" ;

$config = getConfig( $path ) ;
if( $config == FALSE )
{
   printLog( "Failed to parse config/toc.json" ) ;
   exit( 1 ) ;
}

$param = getopt( 'h:m:' ) ;

if( array_key_exists( 'h', $param ) == false )
{
   $param['h'] = "0" ;
}
if( array_key_exists( 'm', $param ) == false )
{
   $param['m'] = "doc" ;
}

if( $param['h'] == "1" )
{
   echo "====== Help! ======\n" ;
   echo "-h  help.\n" ;
   echo "-m  mode: [doc] [pdf] [word] [website] [chm]\n" ;
   exit( 0 ) ;
}

//=== ��ʼ�� ===
printLog( "Init...", "Event" ) ;

$os = getOSInfo() ;
$mdConvert = $os == 'windows' ? 'mdConverter.exe' : 'linux_mdConverter' ;
$html2mysql = $os == 'windows' ? 'exec.bat' : 'exec.sh' ;
$wkhtmltopdf = $os == 'windows' ? 'wkhtmltopdf.exe' : 'wkhtmltopdf' ;

//2.������ļ�
printLog( "Clear file...", "Event" ) ;
if( file_exists( $os == 'windows' ? "$root\build\mid" : "$root/build/mid" ) && removeDir( $os == 'windows' ? "$root\build\mid" : "$root/build/mid" ) == false )
{
   printLog( "Failed to rm dir: $root/build/mid" ) ;
   exit( 1 ) ;
}

if( file_exists( $os == 'windows' ? "$root\build\output" : "$root/build/output" ) && removeDir( $os == 'windows' ? "$root\build\output" : "$root/build/output" ) == false )
{
   printLog( "Failed to rm dir: $root/build/output" ) ;
   exit( 1 ) ;
}

//2.����Ŀ¼
mkdir( "$root/build/mid", 0777, true ) ;
if( file_exists( "$root/build/output/api" ) == false )
{
   mkdir( "$root/build/output/api", 0777, true ) ;
}
chmod( "$root/tools/$mdConvert", 0777 ) ;

//=== Ԥ���� ===
if( $param['m'] == "chm" || $param['m'] == "website" || $param['m'] == "doxygen" )
{
   $file = 'tools/create_javadoc.py' ;
   if( execCmd( "python $root/$file" ) != 0 )
   {
      printLog( "Failed to create javadoc: $file" ) ;
      exit( 1 ) ;
   }
   else
   {
      printLog( "Successfully create javadoc", "Event" ) ;
   }
}

//=== ת�� + ���� ===
//1. pdf
if( $param['m'] == "doc" || $param['m'] == "pdf" )
{
   printLog( "Generate pdf", "Event" ) ;
   if( clearDir( "$root/build/mid" )  == false )
   {
      printLog( 'Failed to clear dir files: build/mid' ) ;
      exit( 1 ) ;
   }

   $pdf = "$root/tools/$mdConvert -v $major -e $minor -d single -s $root/tools/pdfConvertor/src/pdf.css" ;
   if( execCmd( $pdf ) != 0 )
   {
      printLog( 'Failed to convert pdf middle file' ) ;
      exit( 1 ) ;
   }
   $platform = $os == 'windows' ? 'win32' : 'linux64' ;
   chmod( "$root/tools/pdfConvertor/tools/$platform/wkhtmltox/bin/$wkhtmltopdf", 0777 ) ;

   //�޸�����
   $headerContents = file_get_contents( "$root/tools/pdfConvertor/src/header.html" ) ;
   $headerContents = str_replace( '{{version}}', "$major.$minor", $headerContents ) ;
   file_put_contents( "$root/tools/pdfConvertor/src/header_tmp.html", $headerContents ) ;

   $coverContents = file_get_contents( "$root/tools/pdfConvertor/src/cover.html" ) ;
   $coverContents = str_replace( '{{version}}', "$major.$minor", $coverContents ) ;
   file_put_contents( "$root/tools/pdfConvertor/src/cover_tmp.html", $coverContents ) ;

   $pdf = "$root/tools/pdfConvertor/tools/$platform/wkhtmltox/bin/$wkhtmltopdf --page-size A4 --dpi 300 --enable-smart-shrinking --load-error-handling ignore --encoding utf-8 --user-style-sheet $root/tools/pdfConvertor/src/pdf_global.css --footer-html $root/tools/pdfConvertor/src/footer.html --header-html $root/tools/pdfConvertor/src/header_tmp.html --page-offset -1 cover $root/tools/pdfConvertor/src/cover_tmp.html toc --xsl-style-sheet $root/tools/pdfConvertor/src/toc.xsl $root/build/mid/build.html $root/build/output/$outputFileName.pdf" ;
   if( execCmd( $pdf ) != 0 )
   {
      //����pdf��linux�Ĵ���
      if( $os == 'windows' )
      {
         printLog( 'Failed to convert pdf file' ) ;
         exit( 1 ) ;
      }
   }
   printLog( "Finish build pdf document, path: doc/build/output/$outputFileName.pdf", "Event" ) ;
}

//2. word
if( ( $param['m'] == "doc" || $param['m'] == "word" ) && $os == 'windows' )
{
   /*
   printLog( "Generate word", "Event" ) ;
   if( clearDir( "$root/build/mid" )  == false )
   {
      printLog( 'Failed to clear dir files: build/mid' ) ;
      exit( 1 ) ;
   }

   $word = "$root/tools/$mdConvert -v $major -e $minor -d word" ;
   if( execCmd( $word ) != 0 )
   {
      printLog( 'Failed to convert word middle file: '.$word ) ;
      exit( 1 ) ;
   }

   $word = "$root/../java/jdk_win32/bin/java.exe -Dlog4j.configuration=file:$root/tools/wordConvertor/log4j.properties -jar $root/tools/wordConvertor/wordConvertor.jar -i $root/build/mid/build.html -o $root/build/output/$outputFileName.doc -t" ;
   if( execCmd( $word, true ) != 0 )
   {
      printLog( 'Failed to convert word file' ) ;
      exit( 1 ) ;
   }
   printLog( "Finish build word document, path: doc/build/output/$outputFileName.doc", "Event" ) ;
   */
}

//3. chm
if( $param['m'] == "chm" && $os == 'windows' )
{
   printLog( "Generate chm", "Event" ) ;
   if( clearDir( "$root/build/mid" )  == false )
   {
      printLog( 'Failed to clear dir files: build/mid' ) ;
      exit( 1 ) ;
   }

   $chm = "$root/tools/$mdConvert -v $major -e $minor -d chm -l false -s $root/tools/pdfConvertor/src/pdf.css" ;
   if( execCmd( $chm ) != 0 )
   {
      printLog( "Failed to convert chm middle file: $chm" ) ;
      exit( 1 ) ;
   }

   foreach( $apiList as $doxyDoc => $id )
   {
      $source = "$root/build/output/api/$doxyDoc" ;
      //$dest   = getCnPath( $id, $config, "$root/build/mid" ) ;
      $dest   = getDirPath( $id, $config, "$root/build/mid" ) ;
      if( $dest == false )
      {
         printLog( "Failed to find id: $id in toc.json" ) ;
         exit( 1 ) ;
      }
      $dest = "$dest/api/$doxyDoc" ;
      if( $os == 'windows' )
      {
         $dest = iconv( 'utf-8', 'gb2312//IGNORE', $dest ) ;
      }
      copyDir( $source, $dest ) ;
      //echo $source."\n" ;
      //echo iconv( 'utf-8', 'gb2312//IGNORE', $dest )."\n" ;
   }

   $chm = "java -jar $root/tools/chmProjectCreator/chmProjectCreator.jar -i $root/build/mid -o $root/build/output -t \"$outputFileName\" -c $root/config/toc.json" ;
   printLog( "Begin to create chm: $chm" ) ;
   if( execCmd( $chm ) != 0 )
   {
      printLog( "Failed to convert chm config file: $chm" ) ;
      exit( 1 ) ;
   }

   printLog( "Finish build chm config, path: doc/build/output/$outputFileName.wcp", "Event" ) ;

   file_put_contents( "$root/tools/anjian/config.ini", "[config]\r\npath=$root\\build\\output\\$outputFileName.wcp\r\nfile=$outputFileName.wcp" ) ;

   $chm = "$root/tools/anjian/autoBuildCHM.exe" ;
   execCmd( $chm ) ;

   $log = file_get_contents( "$root/tools/anjian/anjian.log" ) ;
   echo "\n".$log."\n" ;
   if( strpos( $log, "Error" ) !== false )
   {
      printLog( 'Failed to build chm.' ) ;
      exit( 1 ) ;
   }

   printLog( "Finish build chm document, path: doc/build/output/$outputFileName.chm", "Event" ) ;
}

//4. ����
if( $param['m'] == "website" )
{
   printLog( "Generate website", "Event" ) ;
   if( clearDir( "$root/build/mid" ) == false )
   {
      printLog( 'Failed to clear dir files: build/mid' ) ;
      exit( 1 ) ;
   }

   $website = "$root/tools/$mdConvert -v $major -e $minor -d website -l false -u false" ;
   if( execCmd( $website ) != 0 )
   {
      printLog( 'Failed to convert website middle file: '.$website ) ;
      exit( 1 ) ;
   }

   $website = "$root/tools/html2mysql/$html2mysql" ;
   chmod( $website, 0777 ) ;
   if( execCmd( $website ) != 0 )
   {
      printLog( 'Failed to send website middle file: '.$website ) ;
      exit( 1 ) ;
   }
   printLog( "Finish build web site", "Event" ) ;
}

echo "Success!\n" ;
exit( 0 ) ;
