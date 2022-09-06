package Tools;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JTextArea;

import org.apache.commons.io.FileUtils;

import burp.BurpExtender;

public class DraggableTextArea extends JTextArea implements DropTargetListener {
        private static final long serialVersionUID = 7247130270544835594L;
      
        public DraggableTextArea() {  
        }  
          
        public void dragEnter(DropTargetDragEvent dtde) {              
        }  
      
        public void dragOver(DropTargetDragEvent dtde) {              
        }  
      
        public void dropActionChanged(DropTargetDragEvent dtde) {               
        }  
      
        public void dragExit(DropTargetEvent dtde) {                
        }
      
        public void drop(DropTargetDropEvent dtde) {
            this.setText("");    
            if(dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)){  
                dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                try {
                    Transferable tr = dtde.getTransferable();  
                    Object obj = tr.getTransferData(DataFlavor.javaFileListFlavor);  
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>)obj;  
                    for(int i = 0; i < files.size(); i++){  
                        append(files.get(i).getAbsolutePath());  
                    }  
                } catch (UnsupportedFlavorException ex) {  
                } catch (IOException ex) {
                }  
            } else if (dtde.isDataFlavorSupported(DataFlavor.stringFlavor) ) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                try {
                    Transferable tr = dtde.getTransferable();  
                    String str=(String) tr.getTransferData(DataFlavor.stringFlavor);  
                    append(str);
                } catch (UnsupportedFlavorException ex) {        
                } catch (IOException ex) {        
                }  
            }
        }
        @Override
        public void setText(String Text) {
        	try {
        		//避免大文件卡死整个burp
				if (Text.length() >= 10000) {
					File tmpFile = new File(FileUtils.getTempDirectory()+File.pathSeparator+"ContentIsInTmpFile.txt");
					FileUtils.writeByteArrayToFile(tmpFile, Text.getBytes());
					Text = tmpFile.getAbsolutePath();
				}
				super.setText(Text);
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
        
        @Override
        public String getText() {
        	String content = super.getText();
        	try {
				if (content.endsWith("ContentIsInTmpFile.txt")) {
					File tempFile = new File(content);
					if (tempFile.exists()){
						content = FileUtils.readFileToString(tempFile);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
        	return content;
        }
        
        
        @Override
        public void insert(String str, int pos) {
        	try {
        		System.out.println("insert called");
        		BurpExtender.getStdout().println("insert called");
				super.insert(str,pos);
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
        
        
        @Override
        public void append(String str) {
        	try {
        		BurpExtender.getStdout().println("append called");
				super.append(str);
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
        
        
        @Override
        public void replaceRange(String str, int start, int end) {
        	try {
        		BurpExtender.getStdout().println("replaceRange called");
				super.replaceRange(str,start,end);
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
}