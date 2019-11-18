#!/usr/bin/python

import shutil
import os
import sys
import platform

rootDir=sys.path[0]
os.chdir(rootDir)

title='Sequoiacm2.1.0'

converterBinName='mdConverter.exe'
if platform.system() == "Linux" : 
   converterBinName='linux_mdConverter'
   
# remove mid dir
midDir=os.path.dirname(rootDir) + os.sep + 'build' + os.sep
shutil.rmtree(midDir, True)

# convert from md to html
cmdConverter = rootDir + os.sep + converterBinName 
print cmdConverter
ret = os.system(cmdConverter)
if ret != 0:
   print 'execute ' + cmdConverter + ' failed'
   sys.exit(1)

# conver from html to chm
chmConverterJar = rootDir + os.sep + 'chmProjectCreator' + os.sep + 'chmProjectCreator.jar'
docDir = rootDir + os.sep + '..';
midDir = docDir + os.sep + 'build' + os.sep + 'mid';
outputDir = docDir + os.sep + 'build' + os.sep + 'output';
tocFile = docDir + os.sep + 'config' + os.sep + 'toc.json';

cmd = 'java -jar ' + chmConverterJar + ' -i ' + midDir + ' -o ' + outputDir + ' -t ' + title + ' -c ' + tocFile
print cmd
os.system(cmd);
