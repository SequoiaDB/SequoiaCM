-------------------使用说明-------------------------------
目的:在linux机器上安装elasticsearch以及tesseract-ocr图片识别引擎
elasticsearch版本：6.3.2
tesseract-ocr:4.1.1

前提要求：
Ubuntu18以上
或Centos7以上
或Red Hat7以上

安装说明:
把install_es文件夹传入目标服务器，进去文件夹执行脚本install_es.sh即可
chmod +x install_es.sh
bash install_es.sh

安装后:
elasticsearch存在位置:/opt/elasticsearch-6.3.2/
(Centos,RedHat)tesseract-ocr存在位置:/opt/tesseract-chi-offline 
（Ubuntu)tesseract-ocr存在位置:/usr/share/tesseract-ocr/
