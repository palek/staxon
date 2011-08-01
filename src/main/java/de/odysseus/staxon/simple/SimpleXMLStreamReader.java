package de.odysseus.staxon.simple;

import java.io.IOException;
import java.io.Reader;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import de.odysseus.staxon.core.AbstractXMLStreamReader;
import de.odysseus.staxon.core.XMLStreamReaderScope;

/**
 * Simple XML Stream Reader
 */
public class SimpleXMLStreamReader extends AbstractXMLStreamReader<Object> {
	private final Reader reader;
	private int ch;
	
	public SimpleXMLStreamReader(Reader reader) throws XMLStreamException {
		super(null);
		this.reader = reader;
		try {
			nextChar();
		} catch (IOException e) {
			throw new XMLStreamException(e);
		}
		init();
	}

	@Override
	protected boolean consume(XMLStreamReaderScope<Object> scope) throws XMLStreamException, IOException {
		skipWhitespace();

		if (ch == -1) {
			readEndDocument();
			return false;
		}

		if (ch == '<') {
			nextChar();
			if (ch == '/') { // END_ELEMENT
				nextChar();
				String tagName = readName('>');
				if (scope.getTagName().equals(tagName)) {
					readEndElementTag();
				} else {
					throw new XMLStreamException("not well-formed");
				}
			} else if (ch == '?') { // START_DOPCUMENT | PROCESSING_INSTRUCTION
				nextChar();
				String target = readName('?');
				String data = null;
				if (ch != '?') {
					data = readText('?');
				}
				nextChar(); // please, let it be '>'
				if ("xml".equals(target)) {
					readStartDocument();
				} else {
					readPI(target, data);
				}
			} else if (ch == '!') { // COMMENT
				StringBuilder comment = new StringBuilder();
				int breakstep = 0;
				do {
					nextChar();
					comment.append(ch);
					if ((ch == '-' && breakstep != 2) || (ch == '>' && breakstep == 2)) {
						breakstep++;
					} else {
						breakstep = 0;
					}
				} while (breakstep < 3);
				readData(comment.substring(0, comment.length() - 3), XMLStreamConstants.COMMENT);
			} else { // START_ELEMENT
				scope = readStartElementTag(readName(' '));
				while (ch != '>' && ch != '/') {
					String name = readName('=');
					nextChar();
					skipWhitespace();
					int quote = ch;
					nextChar();
					String value = readText(quote);
					nextChar();
					skipWhitespace();
					readProperty(name, value);
				}
				if (ch == '/') {
					nextChar(); // please, let it be '>'
					readEndElementTag();
				} else {
					nextChar();
					return consume(scope);
				}
			}
			nextChar();
		} else {
			String text = readText('<');
			readData(text, XMLStreamConstants.CHARACTERS);
		}
		return true;
	}
	

	private void nextChar() throws IOException {
		ch = reader.read();
	}
	
	private void skipWhitespace() throws IOException {
		if (Character.isWhitespace(ch)) {
			do {
				nextChar();
			} while (Character.isWhitespace(ch));
		}
	}
	
	private String readText(final int end) throws IOException {
		final StringBuilder builder = new StringBuilder();
		while (ch != end && ch >= 0) {
			if (ch == '&') {
				nextChar();
				String entity = readName(';');
				if ("lt".equals(entity)) {
					builder.append('<');
				} else if ("gt".equals(entity)) {
					builder.append('>');
				} else if ("amp".equals(entity)) {
					builder.append('&');
				} else if ("quot".equals(entity)) {
					builder.append('"');
				} else if ("apos".equals(entity)) {
					builder.append('\'');
				} else {
					builder.append('?');
				}
			} else {
				builder.append((char)ch);
			}
			nextChar();
		}
		return builder.toString();
	}

	private String readName(final int end) throws IOException {
		final StringBuilder builder = new StringBuilder();
		do {
			builder.append((char)ch);
			nextChar();
		} while (ch != end && ch != '>' && ch != '/' && !Character.isWhitespace(ch));
		skipWhitespace();
		return builder.toString();
	}
}