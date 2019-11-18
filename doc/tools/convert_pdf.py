#!/usr/bin/python

import shutil
import os
import sys
import platform

rootDir=sys.path[0]
os.chdir(rootDir)

title='Sequoiacm2.1.0'

converterBinName='mdConverter.exe'
pdfBin=rootDir + os.sep + 'pdfConvertor' + os.sep + 'tools' + os.sep + 'win64' + os.sep + 'wkhtmltox' + os.sep + 'bin' + os.sep + 'wkhtmltopdf.exe'
if platform.system() == "Linux" : 
   converterBinName='linux_mdConverter'
   pdfBin=rootDir + os.sep + 'pdfConvertor' + os.sep + 'tools' + os.sep + 'linux64' + os.sep + 'wkhtmltox' + os.sep + 'bin' + os.sep + 'wkhtmltopdf'

# remove mid dir
buildDir=os.path.dirname(rootDir) + os.sep + 'build' + os.sep
shutil.rmtree(buildDir, True)

# convert from md to html
cmdConverter = rootDir + os.sep + converterBinName + ' -d word'
print cmdConverter
ret = os.system(cmdConverter)
if ret != 0:
   print 'execute ' + cmdConverter + ' failed'
   sys.exit(1)

# conver from html to pdf
cssFile=rootDir + os.sep + 'pdfConvertor' + os.sep + 'src' + os.sep + 'pdf_global.css'
toclFile=rootDir + os.sep + 'pdfConvertor' + os.sep + 'src' + os.sep + 'toc.xsl'
inputFile=buildDir + os.sep + 'mid' + os.sep + 'build.html'
outputFile=buildDir + os.sep + 'output' + os.sep + title + '.pdf'
cmd=pdfBin + ' --page-size A4 --dpi 300 --enable-smart-shrinking --load-error-handling ignore --encoding utf-8 --user-style-sheet ' + cssFile + ' toc --xsl-style-sheet ' + toclFile + ' ' + inputFile + ' ' + outputFile
print cmd
os.system(cmd);