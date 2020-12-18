import sys
import getopt
import os
from xml.dom.minidom import parse


def update_xml(path='pom.xml', key='scmdriver.version', val='3.1.1'):
    if os.path.isfile(path) is False:
        raise Exception(path + "is not a file")
    dom_tree = parse(path)
    rootNode = dom_tree.getElementsByTagName(key)
    if len(rootNode) is 1:
        rootNode[0].firstChild.data = val
        with open(path, 'w') as f:
            dom_tree.writexml(f, addindent=' ', encoding='utf-8')
    else:
        raise Exception("the key is not found or has more than one,path=" + path + ",key = " + key + ",val = " + val)


def parse_args(argv):
    path = sys.path[0] + os.sep + 'pom.xml'
    key = 'scmdriver.version'
    val = '3.1.1'
    try:
        opts, args = getopt.getopt(argv, 'hf:k:v:', ["help", "file=", "key=", "version="])
    except getopt.GetoptError as e:
        print(e)
        sys.exit(2)
    for opt, arg in opts:
        if opt in ("-h", '--help'):
            print(" usage: python version.py [-f][-k][-v]")
            print(" --file | -f    : the path of pom.xml, default:./pom.xml")
            print(" --key |-k      : the property, default:scmdriver.version")
            print(" --version|-v   : the version,default:3.1.1")
            print(" --help|-h      : list available options")
            sys.exit(2)
        elif opt in ("-f", '--file'):
            path = arg
        elif opt in ("-k", '--key'):
            key = arg
        elif opt in ("-v", '--version'):
            val = arg
    return path, key, val


if __name__ == '__main__':
    path, key, val = parse_args(sys.argv[1:])
    update_xml(path, key, val)
