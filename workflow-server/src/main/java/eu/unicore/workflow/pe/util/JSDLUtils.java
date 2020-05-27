package eu.unicore.workflow.pe.util;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlCursor.TokenType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ApplicationDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ApplicationType;
import org.ggf.schemas.jsdl.x2005.x11.jsdlPosix.POSIXApplicationDocument;

import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;

public class JSDLUtils {
	
	/**
	 * extracts a Posix Application document from the given ApplicationDocument
	 * @param ad
	 * @return POSIXApplicationDocument
	 * returns null in case of errors
	 */
	public static POSIXApplicationDocument extractPosixApplication(ApplicationType ad){
		XmlCursor cursor=ad.newCursor();
		try{
			boolean found=skipToElement(cursor, POSIXApplicationDocument.type.getDocumentElementName());
			if(found){
				return POSIXApplicationDocument.Factory.parse(cursor.newReader());
			}
		}catch(Exception ex){}//ignore
		
		finally{
			if(cursor!=null)cursor.dispose();
		}
		return null;
	}

	/**
	 * set the POSIX application
	 * 
	 * @param pApp - POSIX Application
	 * @param app - JSDL Application
	 */
	public static void setPOSIXApplication(POSIXApplicationDocument pApp, ApplicationDocument app){
		XmlCursor cursor=null;
		try{
			removeXML(POSIXApplicationDocument.type.getDocumentElementName(),app);
			WSUtilities.append(pApp, app.getApplication());
		}
		finally{
			if(cursor!=null)cursor.dispose();
		}
	}
	
	/**
	 * remove a bit of xml 
	 * @param q
	 * @param document
	 */
	public static void removeXML(QName q, XmlObject document){
		XmlCursor cursor=null;
		try{
			cursor=document.newCursor();
			while(true){
				TokenType token=cursor.toNextToken();
				if(token==TokenType.ENDDOC || token==TokenType.ENDDOC){
					break;
				}
				
				if( token==TokenType.START && q.equals(cursor.getName())){
						cursor.removeXml();
				}
			}
			
		}
		finally{
			if(cursor!=null)cursor.dispose();
		}
	}
	
	/**
	 * fast-forward the cursor to the element with the given name
	 *  
	 * @param cursor - the XmlCursor
	 * @param name - the QName of the element to skip to
	 * @return true if element was found
	 */
	public static boolean skipToElement(XmlCursor cursor, QName name){
		while(cursor.hasNextToken()){
			TokenType tt=cursor.toNextToken();
			if(tt.isStart()){
				if(name.equals(cursor.getName())){
					return true;			
				}
			}
		}
		return false;
	}
	
}
