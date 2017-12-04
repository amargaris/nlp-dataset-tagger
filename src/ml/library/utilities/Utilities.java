package ml.library.utilities;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.io.FilenameUtils;

public class Utilities {
	public static final String G=File.separator;
	/**
	 * Shorter reference to the user's desktop
	 */
	public static final String Desk=System.getProperty("user.home")+G+"Desktop"+G;

	public static InputStream getInputStream(String pathOrUrl){
		try {
			return new FileInputStream(pathOrUrl);
		} catch (FileNotFoundException ex){
			try {
				return new URL(pathOrUrl).openStream();
			} catch (IOException ioex) {
				//ioex.printStackTrace();
				return null;
			}
		}
	}
	/**
	 * Method used to open a simple file chooser
	 * @return	The absolute path of the selected file, or null if none is selected.
	 */
	public static String getFile(){
		return getFile(null,null);
	}
	public static String getFile(String title){
		return getFile(title,null);
	}
	public static String getFile(String title,String[] extensions){
		return openDialogForFileOpenOrSave(title, extensions, null,JFileChooser.OPEN_DIALOG);
	}
	public static String getFile(String title,final String[] extensions,String directory){
		return openDialogForFileOpenOrSave(title, extensions, directory, JFileChooser.OPEN_DIALOG);
	}
	public static String saveFile(String title,final String[] extensions,String directory){
		return openDialogForFileOpenOrSave(title, extensions, directory, JFileChooser.SAVE_DIALOG);
	}
	/**
	 * Method used to open a simple file chooser that uses a set of preconfigured extensions as a filter
	 * 
	 * @param extensions String array containing the various possible extensions
	 * @return	The absolute path of the selected file, or null if none is selected.
	 */
	private static String openDialogForFileOpenOrSave(String title,final String[] extensions,String directory,int mode){
		JFileChooser jf = new JFileChooser();
		jf.setCurrentDirectory(directory==null?new File(Desk):new File(directory).exists()?new File(directory):new File(directory).getParentFile());

		if(title!=null)
			jf.setDialogTitle(title);
		if(extensions!=null){
			jf.setFileFilter(new FileFilter() {
				
				@Override
				public String getDescription() {
					return Arrays.toString(extensions);
				}
				
				@Override
				public boolean accept(File arg0) {
					return FilenameUtils.isExtension(arg0.getAbsolutePath(), extensions)||arg0.isDirectory();
				}
			});
		}
		jf.setMultiSelectionEnabled(false);
		jf.setFileSelectionMode(JFileChooser.FILES_ONLY);
		String fullPath=null;
		if(mode==JFileChooser.OPEN_DIALOG){
			if(jf.showOpenDialog(null)==JFileChooser.APPROVE_OPTION){
				fullPath= jf.getSelectedFile().getAbsolutePath();
			}
		} else if(mode==JFileChooser.SAVE_DIALOG){
			if(jf.showSaveDialog(null)==JFileChooser.APPROVE_OPTION){
				fullPath=jf.getSelectedFile().getAbsolutePath();
			}
		}
		if(fullPath==null){
			return fullPath;
		}
		String extension=FilenameUtils.getExtension(fullPath);
		if(extensions==null || extensions.length==0){
			return fullPath;//+extension/*s[0]*/;
		}else{
			for(String myExt:extensions){
				if(myExt.equals(extension)){
					return fullPath;
				}
			}
			return fullPath+extensions[0];
		}
	}
	/**
	 * Simple file chooser method that allows you to select a directory
	 * @return	The absolute path of the directory.
	 */
	public static String getFolder(){
		return getFolder(null);
	}
	public static String getFolder(String text){
		JFileChooser jf = new JFileChooser();
		jf.setMultiSelectionEnabled(false);
		if(text!=null){
			jf.setDialogTitle(text);
		}
		jf.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		jf.setCurrentDirectory(new File(Desk));
		if(jf.showOpenDialog(null)==JFileChooser.APPROVE_OPTION){
			return jf.getSelectedFile().getAbsolutePath();
		}
		return null;
	}
	public static String[] showListedDialogMultiple(String title,String[] inputNames){
		return showListedDialogGeneral(title, inputNames, true);
	}
	public static String showListedDialogSingle(String title,String[] inputNames){
		return showListedDialogGeneral(title, inputNames, false)[0];
	}
	/**
	 * This method generates a String/ Multi-String selection dialog from a given preset of possible Strings.
	 * Blocks the thread
	 * @param inputNames	The superset of possible Strings.
	 * @return The selected Strings (sub-set of names)
	 */
	private static String[] showListedDialogGeneral(String title,final String[] inputNames,boolean isMultiple/*,final Task<String[]>task*/){
		final List<String>theResult= new ArrayList<String>();
		final DefaultListModel<String> model = new DefaultListModel<>();
		Arrays.asList(inputNames).forEach(model::addElement);
		
		final JList<String> nameList= new JList<>(model);

		nameList.setSelectionMode(!isMultiple?ListSelectionModel.SINGLE_SELECTION:ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		nameList.setSelectedIndex(0);
		
		JButton apply = new JButton("Apply");
		apply.addActionListener((e)-> {
			for(String s:nameList.getSelectedValuesList()){
				theResult.add(s);
			}
			synchronized(theResult){
				theResult.notifyAll();
			}
		});
		
		JPanel buttons = new JPanel(new GridLayout(1,0));
		buttons.setOpaque(false);
		buttons.add(apply);
		if(isMultiple){
			JButton all= new JButton("Select All");
			all.addActionListener(e-> {
				int[] intAr = new int[inputNames.length];
				for(int i=0;i<intAr.length;i++){
					intAr[i]=i;
				}
				nameList.setSelectedIndices(intAr);
			});
			buttons.add(all);
			JButton none = new JButton("Deselect All");
			none.addActionListener((e)-> nameList.clearSelection());
			buttons.add(none);
		}
		final long[] vals = new long[]{Long.MAX_VALUE};
		final JTextArea nodeFinder = new JTextArea();
		nodeFinder.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) { insertUpdate(e); }
			@Override
			public void changedUpdate(DocumentEvent e) { insertUpdate(e); }
			@Override
			public void insertUpdate(DocumentEvent e) {
				vals[0]=System.currentTimeMillis();
			}
		});
		final long THRESHOLD=1000;
		
		final JPanel merged = new JPanel(new BorderLayout());
		new Thread(()->{
			try{
				Thread.sleep(500);
			}catch(Exception ee){
				
			}
			while(nodeFinder.isDisplayable()){
				try{
					Thread.sleep(250);
					if(vals[0]==Long.MAX_VALUE){
						continue;
					}
					long time =System.currentTimeMillis();
					if(time-vals[0]>THRESHOLD){
						String nodeName=nodeFinder.getText();
						if(nodeName.length()>3){
							List<String> newList = Arrays.asList(inputNames).stream().filter((i)->i.contains(nodeName)).collect(Collectors.toList());
							List<String> newOldList = Arrays.asList(inputNames).stream().filter((i)->!i.contains(nodeName)).collect(Collectors.toList());
							SwingUtilities.invokeLater(()-> {
								nameList.invalidate();
								for(String s:newOldList){
									model.removeElement(s);
								}
								for(String s:newList){
									if(!model.contains(s))
										model.addElement(s);
								}
								nameList.validate();
								nameList.repaint();
								newList.stream().map((s)->Arrays.asList(inputNames).indexOf(s)).collect(Collectors.toList());
								int[] intAr = new int[newList.size()];
								for(int j=0;j<newList.size();j++){
									intAr[j] = newList.indexOf(newList.get(j));
								}
								nameList.clearSelection();
								nameList.setSelectedIndices(intAr);
							});
						}else{
							List<String> newList = Arrays.asList(inputNames);
							List<String> newOldList = Arrays.asList(inputNames);
							SwingUtilities.invokeLater(()-> {
								nameList.invalidate();
								for(String s:newOldList){
									if(model.contains(s))
										model.removeElement(s);
								}
								for(String s:newList){
									if(!model.contains(s))
										model.addElement(s);
								}
								nameList.validate();
								nameList.repaint();
								nameList.clearSelection();
							});
							nameList.clearSelection();
						}
						vals[0]=Long.MAX_VALUE;
					}
				}catch(Exception ee){
					
				}
			}
		}).start();
		
		merged.setOpaque(false);
		nodeFinder.setPreferredSize(new Dimension(1,30));
		merged.add(nodeFinder,BorderLayout.NORTH);
		merged.add(new JScrollPane(nameList),BorderLayout.CENTER);
		merged.add(buttons,BorderLayout.SOUTH);
		JFrame frame=displayIntoFrame(merged, title);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		WindowAdapter listener=new WindowAdapter(){
			public void windowClosing(WindowEvent ev){
				synchronized(theResult){
					theResult.notifyAll();
				}
			}
		};
		frame.addWindowListener(listener);
		try {
			synchronized(theResult){
				theResult.wait();
			}
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		frame.removeWindowListener(listener);
		SwingUtilities.invokeLater(frame::dispose);
		return theResult.size()!=0?theResult.toArray(new String[0]):new String[0];
	}
	public static int displayIntoDialog(Component comp,String message){
		return JOptionPane.showConfirmDialog(null, comp, message,JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
	}
	public static JFrame displayIntoFrame(JComponent component,String title,int width,int height){
		final JFrame fram = new JFrame(title);
		fram.setSize(width,height);
		fram.setLocationRelativeTo(null);
		fram.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		if(component!=null){
			fram.getContentPane().add(component,BorderLayout.CENTER);
		}
		fram.getContentPane().setBackground(Color.white);
		SwingUtilities.invokeLater(()->fram.setVisible(true));
		return fram;
	}
	/**
	 * Simple method that displays a component inside a newly generated JFrame
	 * @param component	The component to be displayed.
	 * @param title	The title of the window
	 * @return	Reference to the JFrame object for further customization
	 */
	public static JFrame displayIntoFrame(JComponent component,String title){
		return displayIntoFrame(component, title, 400, 400);
	}
	public static JFrame displayIntoFrame(String title){
		return displayIntoFrame(null, title);
	}
}
