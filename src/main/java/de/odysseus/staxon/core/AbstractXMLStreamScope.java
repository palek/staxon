package de.odysseus.staxon.core;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;


/**
 * Represent document/element scope. Used to store namespace declarations and
 * attributes, implements {@link NamespaceContext}.
 */
public class AbstractXMLStreamScope implements NamespaceContext {
	private final NamespaceContext parent;
	private final String prefix;
	private final String localName;
	private final String tagName;

	private String defaultNamespace;
	private List<Pair<String, String>> prefixes;
	private AbstractXMLStreamScope lastChild;
	private boolean startTagClosed;

	/**
	 * Create root scope.
	 * 
	 * @param defaultNamespace
	 */
	public AbstractXMLStreamScope(String defaultNamespace) {
		this.parent = null;
		this.prefix = null;
		this.localName = null;
		this.tagName = null;
		this.defaultNamespace = defaultNamespace;
		this.startTagClosed = true;
	}

	/**
	 * Create root scope.
	 * 
	 * @param parent
	 *            root namespace context
	 */
	public AbstractXMLStreamScope(NamespaceContext parent) {
		this.parent = parent;
		this.prefix = null;
		this.localName = null;
		this.tagName = null;
		this.defaultNamespace = parent.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX);
		this.startTagClosed = true;
	}

	/**
	 * Create element scope.
	 * 
	 * @param parent
	 * @param prefix
	 * @param localName
	 */
	public AbstractXMLStreamScope(AbstractXMLStreamScope parent, String prefix, String localName) {
		this.parent = parent;
		this.prefix = prefix;
		this.localName = localName;
		this.startTagClosed = false;
		
		tagName = XMLConstants.DEFAULT_NS_PREFIX.equals(prefix) ? localName : prefix + ':' + localName;
		defaultNamespace = parent.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX);
		parent.lastChild = this;
		parent.startTagClosed = true;
	}

	public String getPrefix() {
		return prefix;
	}
	
	public String getLocalName() {
		return localName;
	}
	
	public String getTagName() {
		return tagName;
	}

	public boolean isRoot() {
		return tagName == null;
	}

	public AbstractXMLStreamScope getParent() {
		return isRoot() ? null : (AbstractXMLStreamScope)parent;
	}

	public AbstractXMLStreamScope getLastChild() {
		return lastChild;
	}

	public boolean isStartTagClosed() {
		return startTagClosed;
	}

	void setStartTagClosed(boolean startTagClosed) {
		this.startTagClosed = startTagClosed;
	}

	public String getPrefix(String namespaceURI) {
		if (namespaceURI == null) {
			throw new IllegalArgumentException();
		} else if (namespaceURI.equals(defaultNamespace)) {
			return XMLConstants.DEFAULT_NS_PREFIX;
		} else if (XMLConstants.XML_NS_URI.equals(namespaceURI)) {
			return XMLConstants.XML_NS_PREFIX;
		} else if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceURI)) {
			return XMLConstants.XMLNS_ATTRIBUTE;
		} else {
			if (prefixes != null) {
				for (Pair<String, String> pair : prefixes) {
					if (pair.getSecond().equals(namespaceURI)) {
						return pair.getFirst();
					}
				}
			}
			return parent == null ? null : parent.getPrefix(namespaceURI);
		}
	}

	public void setPrefix(String prefix, String namespaceURI) {
		if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
			defaultNamespace = namespaceURI;
		} else if (XMLConstants.XML_NS_PREFIX.equals(namespaceURI)) {
			throw new IllegalArgumentException();
		} else if (XMLConstants.XMLNS_ATTRIBUTE.equals(namespaceURI)) {
			throw new IllegalArgumentException();
		} else {
			if (prefixes == null) {
				prefixes = new LinkedList<Pair<String, String>>();
			} else {
				Iterator<Pair<String, String>> iterator = prefixes.iterator();
				while (iterator.hasNext()) {
					if (iterator.next().getFirst().equals(prefix)) {
						iterator.remove();
					}
				}
			}
			prefixes.add(new Pair<String, String>(prefix, namespaceURI));
		}
	}

	public Iterator<String> getPrefixes(final String namespaceURI) {
		if (namespaceURI == null) {
			throw new IllegalArgumentException();
		} else if (XMLConstants.XML_NS_URI.equals(namespaceURI)) {
			return Arrays.asList(XMLConstants.XML_NS_PREFIX).iterator();
		} else if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceURI)) {
			return Arrays.asList(XMLConstants.XMLNS_ATTRIBUTE).iterator();
		} else {
			return new Iterator<String>() {
				int state = 0;
				String next = null;
				Iterator<Pair<String, String>> pairs;
				Iterator<?> above;

				private String next0() {
					switch (state) {
					case 0: // check default
						if (namespaceURI.equals(defaultNamespace)) {
							state = 1;
							return XMLConstants.DEFAULT_NS_PREFIX;
						}
					case 1: // check pairs
						state = 1;
						if (prefixes != null) {
							if (pairs == null) {
								pairs = prefixes.iterator();
							}
							Pair<String, String> p;
							while (pairs.hasNext()) {
								p = pairs.next();
								if (namespaceURI.equals(p.getSecond())) {
									return p.getFirst();
								}
							}
						}
					case 2: // check above
						state = 2;
						if (parent != null) {
							if (above == null) {
								above = parent.getPrefixes(namespaceURI);
							}
							if (above.hasNext()) {
								return above.next().toString();
							}
						}
					default:
						state = -1;
						return null;
					}
				}

				public boolean hasNext() {
					if (next == null) {
						next = next0();
					}
					return next != null;
				}

				public String next() {
					if (!hasNext()) {
						throw new NoSuchElementException();
					}
					String result = next;
					next = null;
					return result;
				}

				public void remove() {
					throw new UnsupportedOperationException("Cannot remove prefix");
				}
			};
		}
	}

	public String getNamespaceURI(String prefix) {
		if (prefix == null) {
			throw new IllegalArgumentException();
		} else if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
			return defaultNamespace;
		} else if (XMLConstants.XML_NS_PREFIX.equals(prefix)) {
			return XMLConstants.XML_NS_URI;
		} else if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
			return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
		} else {
			if (prefixes != null) {
				for (Pair<String, String> pair : prefixes) {
					if (pair.getFirst().equals(prefix)) {
						return pair.getSecond();
					}
				}
			}
			return parent == null ? XMLConstants.NULL_NS_URI : parent.getNamespaceURI(prefix);
		}
	}
}