package com.adaptris.utils;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import javax.print.Doc;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author mwarman
 */
public class XpathUtils {

  private static final String UNIQUE_ID_KEY = "unique-id";

  private static final String[] UNIQUE_ID_PARENT = {"channel-list", "workflow-list", "services", "connections"};

  private XpathUtils(){
  }

  public static Document createDocument(String file) throws IOException, SAXException, ParserConfigurationException {
    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
    return docBuilder.parse(new ByteArrayInputStream((file).getBytes()));
  }

  public static List<String> getXPath(Document document, String textContents, int expectedCount) {
    List<Node> nodes = new ArrayList<>();
    while(expectedCount != nodes.size()){
      nodes.add(getXPath(document.getDocumentElement(), textContents, nodes));
    }
    List<String> xpaths = new ArrayList<>();
    for (Node node : nodes){
      String xpath = generateXPath(node);
      if(!xpath.isEmpty()) {
        xpaths.add(xpath);
      }
    }
    return xpaths;
  }

  private static Node getXPath(Node root, String textContents, List<Node> foundNodes) {
    for (int i = 0; i < root.getChildNodes().getLength(); i++) {
      Node node = root.getChildNodes().item(i);
      if (node instanceof Text && ((Text) node).getWholeText().contains(textContents) && !foundNodes.contains(node.getParentNode())){
        return node.getParentNode();
      } else if (node instanceof Element && node.getChildNodes().getLength() > 0) {
        Node result = getXPath(node, textContents, foundNodes);
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }

  private static String generateXPath(Node node){
    Node parent = node.getParentNode();
    if (parent == null)
    {
      return "";
    }
    String uniqueId = null;
    for (int i = 0; i < node.getChildNodes().getLength(); i++) {
      Node child = node.getChildNodes().item(i);
      if(child instanceof Element){
        if (child.getNodeName().equals(UNIQUE_ID_KEY)){
          if (Arrays.asList(UNIQUE_ID_PARENT).contains(node.getParentNode().getNodeName())) {
            uniqueId = child.getTextContent();
          }
        }
      }
    }
    int pos  = 0;
    int siblings  = 0;
    if(node.getParentNode().getChildNodes().getLength() > 1 && uniqueId == null){
      for (int i = 0; i < node.getParentNode().getChildNodes().getLength(); i++) {
        Node child = node.getParentNode().getChildNodes().item(i);
        if(child instanceof Element){
          if (child.getNodeName().equals(node.getNodeName())){
            siblings++;
          }
          if (child.equals(node)){
            pos = siblings;
          }
        }
      }
    }
    return generateXPath(parent) + "/" + node.getNodeName() +
        (uniqueId != null ? String.format("[" + UNIQUE_ID_KEY + "=\"%s\"]", uniqueId): "") +
        (siblings > 1 ? String.format("[%s]", pos) : "");
  }

  public static String evaluateXpath(Document xml, String expression) throws XPathExpressionException {
    XPathFactory xpf = XPathFactory.newInstance();
    XPath xpath = xpf.newXPath();
    return (String) xpath.evaluate(expression, xml, XPathConstants.STRING);
  }

}
