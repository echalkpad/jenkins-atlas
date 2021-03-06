#!/usr/bin/env jython

from xml.dom.minidom import parse,parseString
from xml.dom import Node
import xml.dom.minidom
import simplejson as json
import sys
import re
import os

def printf(format,*args): sys.stdout.write(format%args)

def fprintf(fp,format,*args): fp.write(format%args)

def getNodeMap():
    mapOut = {}
    nodeTypes = ['ATTRIBUTE_NODE', 'CDATA_SECTION_NODE', 'COMMENT_NODE',
                 'DOCUMENT_FRAGMENT_NODE', 'DOCUMENT_NODE',
                 'DOCUMENT_TYPE_NODE', 'ELEMENT_NODE', 'ENTITY_NODE',
                 'ENTITY_REFERENCE_NODE', 'NOTATION_NODE',
                 'PROCESSING_INSTRUCTION_NODE', 'TEXT_NODE' ]
    for n in nodeTypes:
        k = getattr(Node,n)
        v = n
        mapOut[k]=v
    return mapOut

def fullPath(file_path):
    full_path = os.path.expanduser(file_path)
    full_path = os.path.abspath(file_path)
    return full_path


def load_json(json_file):
    fp = open(fullPath(full_path),"r")
    json_data = fp.read()
    fp.close()
    out = json.loads(json_data)
    return out

def save_json(json_file,obj):
    fp = open(fullPath(json_file),"w")
    out = json.dumps(obj, indent=2)
    fp.write(out)
    fp.close()

def nodeAttributes(node):
    if node.attributes:
        return dict([(key,val) for (key,val) in node.attributes.items()])
    return {}

def getXMLDOC(file_name):
    fp = open(file_name)
    xmlData = fp.read()
    doc = parseString(xmlData)
    fp.close()
    return doc

def filterNodeChildrenByNodeType(nodeIn,nodeType):
    nodesOut = []
    for childNode in nodeIn.childNodes:
        try:
            nodeTypeStr = nodeMap[childNode.nodeType]
        except KeyError:
            continue
        if nodeTypeStr == nodeType:
            nodesOut.append(childNode)
    return nodesOut

def parseAndSave(xmlFileName,jsonFileName,entitiesPy,persistName):
    classes = []
    config = {"classes":classes}
    doc = getXMLDOC(xmlFileName)
    classNodes = doc.getElementsByTagName("class")
    efp = open(fullPath(entitiesPy),"w")

    for classNode in classNodes:
        textNodes = filterNodeChildrenByNodeType(classNode,"TEXT_NODE")
        if len(textNodes)<1:
            printf("class Node had no text skipping\n")
            continue
        className = textNodes[0].nodeValue
        p = classNode.parentNode
        if p.nodeName != PU:
            printf("Stray class %s not contained in %s skipping\n",className,PU)
            continue
        attrs = nodeAttributes(p)
        try:
            nodeUnitName = attrs["name"]
        except KeyError:
            fmt = "class %s belongs to a %s tag with no name. skipping\n"
            printf(fmt,className,PU)
            continue
        if nodeUnitName != persistName:
            fmt = "class %s belongs to unit %s"
            fmt += " but was looking for %s. skipping\n"
            printf(fmt,className,nodeUnitName,persistName)
            continue
        printf("Including %s in %s\n",className,jsonFileName)
        classes.append(className)
        classNameTail = className.split(".")[-1]
        efp.write("import %s as %s\n"%(className,classNameTail))
    efp.close()
    save_json(jsonFileName,config)

def usage(prog):
    printf("Usage is %s <persitence.xml> <conf.json> <entities py file>",prog)
    printf(" <persitence unit name>\n")
    printf("\n")
    printf("searches the persistence.xml file for a persistence-unit\n")
    printf("that matches the name specified on the command line\n")
    printf("And converts it to a json file that can assigned to the\n")
    printf("database->mapfile option in your main json config file\n")
    printf("\n")



PU = "persistence-unit"
nodeMap = getNodeMap()

if __name__ == "__main__":
    prog = os.path.basename(sys.argv[0])
    if len(sys.argv) < 5:
        usage(prog)
        sys.exit()

    xmlFileName = sys.argv[1]
    jsonFileName = sys.argv[2]
    entitiesPy = sys.argv[3]
    persistName = sys.argv[4]

    parseAndSave(xmlFileName,jsonFileName,entitiesPy,persistName)
	
