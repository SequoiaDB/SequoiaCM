#!/bin/bash
rootDir=$(cd "$(dirname "$0")";pwd)

function unzip_es()
{
  unzip -n -d /opt $rootDir/elasticsearch-6.3.2.zip
  res=$?
  if [ $res -eq 0 ] ; then
      unzip -n -d /opt/elasticsearch-6.3.2/plugins/ik $rootDir/elasticsearch-analysis-ik-6.3.2.zip
      str=`cat /opt/elasticsearch-6.3.2/config/elasticsearch.yml | grep 'network.host: 0.0.0.0'`
      echo $str
      if [ -z "$str" ] ;then
          echo 'network.host: 0.0.0.0' >>/opt/elasticsearch-6.3.2/config/elasticsearch.yml
          echo 'action.auto_create_index: .watches,.triggered_watches,.watcher-history-*' >>/opt/elasticsearch-6.3.2/config/elasticsearch.yml
      fi
  else
      echo "****************************unzip failed********************"
      exit 1
  fi 
}

function install_es()
{
    conf=`cat /etc/security/limits.conf | grep 'sequoiadb soft'`
    if [ -z "$conf" ] ;then
        echo 'sequoiadb soft 65536' >>/etc/security/limits.conf
        echo 'sequoiadb hard 65536' >>/etc/security/limits.conf
    fi
    str=`cat /etc/sysctl.conf | grep 'vm.max_map_count='`
    if [ -z "$str" ] ;then
        echo 'vm.max_map_count=655360' >>/etc/sysctl.conf
        sysctl -p
    fi
    chown sequoiadb -R /opt/elasticsearch-6.3.2
    su - sequoiadb -c "/opt/elasticsearch-6.3.2/bin/elasticsearch -d" 
    res=$?
    if [ $res -eq 0 ] ; then
        echo "Please wait,startuping..."
        sleep 10s
        echo "-------------------------------succeed to startup  elasticsearch---------------------"
    else
        echo "****************************failed to startup  elasticsearch********************"
        exit 1
    fi
}

function install_tesseract()
{
    #CentOS
    cat /etc/redhat-release | grep 'CentOS'
    res_cs=$?
    if [ $res_cs -eq 0 ] ; then
        tar -zxvf $rootDir/tesseract-chi-offline.tar.gz -C /opt
        cp $rootDir/test.PNG /opt/tesseract-chi-offline/
        cd /opt/tesseract-chi-offline/
        rpm -Uvh --force --nodeps *.rpm
        tesseract test.PNG  output -l eng+chi_sim
        res=$?
        if [ $res -eq 0 ] ; then
            if [ -s "/opt/tesseract-chi-offline/output.txt" ] ; then
                echo "-------------------------------succeed to install tesseract-ocr ---------------------"
                exit 0
            else 
                echo "****************************failed to install tesseract-ocr ********************"
                exit 1 
            fi
        else
            echo "****************************failed to install tesseract-ocr ********************"
            exit 1
        fi
    fi
    #Ubuntu
    cat /etc/lsb-release | grep 'Ubuntu'
    res_ubu=$?
    if [ $res_ubu -eq 0 ] ; then
        apt-get install -f tesseract-ocr
        cd /usr/share/tesseract-ocr/tessdata/
        cp $rootDir/chi_sim.traineddata /usr/share/tesseract-ocr/tessdata/
        cp $rootDir/test.PNG /usr/share/tesseract-ocr/tessdata/
        tesseract $rootDir/test.PNG  output -l eng+chi_sim
        res=$?
        if [ $res -eq 0 ] ; then
            if [ -s "/usr/share/tesseract-ocr/tessdata/output.txt" ] ; then
                echo "-------------------------------succeed to install tesseract-ocr ---------------------"
                exit 0
            else 
                echo "****************************failed to install tesseract-ocr ********************"
                exit 1 
            fi
        else
            echo "****************************failed to install tesseract-ocr ********************"
            exit 1
        fi
    fi
    #Red Hat
    cat /etc/redhat-release | grep 'Red Hat'
    res_red=$?
    if [ $res_red -eq 0 ] ; then
        tar -zxvf $rootDir/tesseract-chi-offline.tar.gz -C /opt
        cp $rootDir/test.PNG /opt/tesseract-chi-offline/
        cd /opt/tesseract-chi-offline/
        rpm -Uvh --force --nodeps *.rpm
        tesseract test.PNG  output -l eng+chi_sim
        res=$?
        if [ $res -eq 0 ] ; then
            if [ -s "/opt/tesseract-chi-offline/output.txt" ] ; then
                echo "-------------------------------succeed to install tesseract-ocr ---------------------"
                exit 0
            else 
                echo "****************************failed to install tesseract-ocr ********************"
                exit 1 
            fi
        else
            echo "****************************failed to install tesseract-ocr ********************"
            exit 1
        fi
    fi
}

unzip_es
install_es
install_tesseract
