package de.tuberlin.dima.schubotz.fse;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


// TODO: Auto-generated Javadoc
/**
 * The Class XMLHelper.
 */
@SuppressWarnings("UnusedDeclaration")
public final class XMLHelper {
	
	/** The factory. */
	private static XPathFactory factory = XPathFactory.newInstance();
	
	/** The xpath. */
	private static XPath xpath = factory.newXPath();


    /**
	 * Compact form.
	 *
	 * @param node the node
	 * @return the string
	 */
	public static String CompactForm(Node node){
		Mynode n = new Mynode(node,null);
		return CompactForm(n).out;
	}
	
	/**
	 * Compact form.
	 *
	 * @param n the n
	 * @return the mynode
	 */
	private static Mynode CompactForm(Mynode n){
		if(n.node.getNodeType()==Node.TEXT_NODE){
			n.out=n.node.getNodeValue().trim();
		} else {
			 n.out = n.node.getNodeName();
				if(n.out.startsWith("m:"))
					n.out=n.out.substring(2);
				if(n.out.equals("annotation-xml"))
					n.out="";
				if(n.out.equals("pquery")||n.out.equals("cquery"))
					n.out="";
				if(n.out.equals("mws:qvar")){
					String qname = n.node.getAttributes().getNamedItem("name").toString();				
					if(n.qVar == null){
						n.out="MYQVARNEW";
                        n.qVar = new HashMap<>();
                        n.qVar.put(qname, 1);
                    } else {
						Integer qInt = n.qVar.get(qname);
						if(qInt==null){
							n.out="MYQVARNEW";
							n.qVar.put(qname, n.qVar.size());
						} else {
							n.out="MYQVAR"+qInt+"OLD";
						}
					}
				}
				if(n.node.hasChildNodes()){
                    String sChild = "";
                    for (Node childNode = n.node.getFirstChild();
                         childNode!=null; childNode=childNode.getNextSibling()){
						Mynode ret = CompactForm(new Mynode(childNode, n.qVar));
						String cn = ret.out;
						n.qVar=ret.qVar;
						if(cn.length()>0)
							sChild+=cn+";";
					}
					if(sChild.endsWith(";")){
						sChild=sChild.substring(0, sChild.length()-1).trim();
					}
					if(sChild.length()>0&&sChild.codePointAt(0)!=8290&&sChild.codePointAt(0)!=8289  ){
						if(n.node.getNodeName().equals("m:annotation-xml"))
							n.out+=sChild;
							else
						n.out+="["+sChild+"]";
						
					}
				}
		}
		if(n.out.equals("mo"))
			n.out=""; //Remove empty mo elements
		return n;
		
	}
	
	/**
	 * Helper program: Extracts the specified XPATH expression
	 * from an XML-String.
	 *
	 * @param InputXMLString the input xml string
	 * @param XPath the x path
	 * @return NodeList
	 * @throws ParserConfigurationException the parser configuration exception
	 * @throws SAXException the sAX exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws XPathExpressionException the x path expression exception
	 */
	public static NodeList String2NodeList(String InputXMLString,
			String XPath) throws ParserConfigurationException, SAXException,
			IOException, XPathExpressionException {

		Document doc = String2Doc(InputXMLString, false);
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		XPathExpression expr = xpath.compile(XPath);

		Object result = expr.evaluate(doc, XPathConstants.NODESET);
		return (NodeList) result;

	}
	
	/**
	 * Helper program: Extracts the specified XPATH expression
	 * from an XML-String.
	 *
	 * @param node the node
	 * @param XPath the x path
	 * @return NodeList
	 * @throws ParserConfigurationException the parser configuration exception
	 * @throws SAXException the sAX exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws XPathExpressionException the x path expression exception
	 */
	public static Node getElementB(Node node, String XPath) throws ParserConfigurationException, SAXException,
			IOException, XPathExpressionException {


		XPathExpression expr = xpath.compile(XPath);
		return getElementB(node, expr);

	}
	
	/**
	 * Helper program: Extracts the specified XPATH expression
	 * from an XML-String.
	 *
	 * @param node the node
	 * @param XPath the x path
	 * @return NodeList
	 * @throws ParserConfigurationException the parser configuration exception
	 * @throws SAXException the sAX exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws XPathExpressionException the x path expression exception
	 */
	public static Node getElementB(Node node, XPathExpression XPath) throws ParserConfigurationException, SAXException,
			IOException, XPathExpressionException {

        return getElementsB(node, XPath).item(0);

	}

    /**
     * Helper program: Extracts the specified XPATH expression
     * from an XML-String.
     *
     * @param node  the node
     * @param XPath the x path
     * @return NodeList
     * @throws ParserConfigurationException the parser configuration exception
     * @throws SAXException                 the sAX exception
     * @throws IOException                  Signals that an I/O exception has occurred.
     * @throws XPathExpressionException     the x path expression exception
     */
    public static NodeList getElementsB(Node node, XPathExpression XPath) throws ParserConfigurationException, SAXException,
            IOException, XPathExpressionException {

        return (NodeList) XPath.evaluate(node, XPathConstants.NODESET);

    }
    /**
     * Helper program: Extracts the specified XPATH expression
     * from an XML-String.
     *
     * @param node  the node
     * @param xString the x path
     * @return NodeList
     * @throws ParserConfigurationException the parser configuration exception
     * @throws SAXException                 the sAX exception
     * @throws IOException                  Signals that an I/O exception has occurred.
     * @throws XPathExpressionException     the x path expression exception
     */
    public static NodeList getElementsB(Node node, String xString) throws ParserConfigurationException, SAXException,
            IOException, XPathExpressionException {
        XPathExpression xPath = compileX(xString);
        return (NodeList) xPath.evaluate(node, XPathConstants.NODESET);

    }
    /**
	 * Helper program: Transforms a String to a XML Document.
	 *
	 * @param InputXMLString the input xml string
	 * @param NamespaceAwareness the namespace awareness
	 * @return parsed document
	 * @throws ParserConfigurationException the parser configuration exception
	 * @throws SAXException the sAX exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */

	public static Document String2Doc(String InputXMLString, boolean NamespaceAwareness)
			throws ParserConfigurationException, SAXException, IOException{
		DocumentBuilderFactory domFactory = DocumentBuilderFactory
				.newInstance();
		 domFactory.setNamespaceAware(NamespaceAwareness);
		 //domFactory.setValidating(true);
        domFactory.setAttribute(
                "http://apache.org/xml/features/dom/include-ignorable-whitespace",
                Boolean.FALSE);
        domFactory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = domFactory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(InputXMLString));
		is.setEncoding("UTF-8"); 
		return  builder.parse(is);

	}
    /**
     * Returns a list of unique identifiers from a MathML string.
     * This function searches for all <mi/> or <ci/> tags within
     * the string.
     * 
     * @param mathml 
     * @return a list of unique identifiers. When no identifiers were
     *         found, an empty list will be returned.
     */
    @SuppressWarnings("JavaDoc")
    public static Multiset<String> getIdentifiersFrom( String mathml ) {
        Multiset<String> list = HashMultiset.create();
        Pattern p = Pattern.compile( "<((m:)?[mc][ion])(.*?)>(.{1,4}?)</\\1>", Pattern.DOTALL );
        Matcher m = p.matcher( mathml );
        while ( m.find() ) {
            String identifier = m.group( 4 );
            list.add( identifier );
        }
        return list;
    }
    /**
     * Returns a list of unique identifiers from a MathML string.
     * This function searches for all <mi/> or <ci/> tags within
     * the string.
     *
     * @param mathml
     * @return a list of unique identifiers. When no identifiers were
     *         found, an empty list will be returned.
     */
    @SuppressWarnings("JavaDoc")
    public static Multiset<String> getIdentifiersFromQuery( String mathml ) {
        Multiset<String> list = HashMultiset.create();
        Pattern p = Pattern.compile( "[mc][ion]\\[([^\\]]{1,4})\\]" );
        Matcher m = p.matcher( mathml );
        while ( m.find() ) {
            String identifier = m.group( 1 );
            list.add( identifier );
        }
        return list;
    }

    /*the document.
     *
	 * @param doc the doc
	 * @return the string
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws TransformerException the transformer exception
	 */
    public static String printDocument(Node doc) throws IOException, TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        StringWriter sw = new StringWriter();
        transformer.transform(new DOMSource(doc),
                new StreamResult(sw));
        return sw.toString();
    }

    /**
     * Prie x.
     *
     * @param xString the x string
     * @return the x path expression
     * @throws XPathExpressionException the x path expression exception
     */
    public static XPathExpression compileX(String xString) throws XPathExpressionException {
        return xpath.compile(xString);
    }

    /**
     * The Class Mynode.
     */
    private static class Mynode {

        /**
         * The node.
         */
        public Node node;

        /**
         * The q var.
         */
        public Map<String, Integer> qVar;

        /**
         * The out.
         */
        public String out;

        /**
         * Instantiates a new mynode.
         *
         * @param node the node
         * @param qVar the q var
         */
        public Mynode(Node node, Map<String, Integer> qVar) {
            this.node = node;
            this.qVar = qVar;
        }
    }

    /**
     * Compil
     * /**
     * The Class NdLst.
     */
    @SuppressWarnings("UnusedDeclaration")
    public static class NdLst implements NodeList, Iterable<Node> {

        /**
         * The nodes.
         */
        private List<Node> nodes;

        /**
         * Instantiates a new nd lst.
         *
         * @param list the list
         */
        public NdLst(NodeList list) {
            nodes = new ArrayList<>();
            for (int i = 0; i < list.getLength(); i++) {
                if (!isWhitespaceNode(list.item(i))) {
                    nodes.add(list.item(i));
                }
            }
        }

        /**
         * Checks if is whitespace node.
         *
         * @param n the n
         * @return true, if is whitespace node
         */
        private static boolean isWhitespaceNode(Node n) {
            if (n.getNodeType() == Node.TEXT_NODE) {
                String val = n.getNodeValue();
                return val.trim().length() == 0;
            } else {
                return false;
            }
        }

        /* (non-Javadoc)
         * @see org.w3c.dom.NodeList#item(int)
         */
        public Node item(int index) {
            return nodes.get(index);
        }

        /* (non-Javadoc)
         * @see org.w3c.dom.NodeList#getLength()
         */
        public int getLength() {
            return nodes.size();
        }

        /* (non-Javadoc)
         * @see java.lang.Iterable#iterator()
         */
        public Iterator<Node> iterator() {
            return nodes.iterator();
        }
    }

	
}
