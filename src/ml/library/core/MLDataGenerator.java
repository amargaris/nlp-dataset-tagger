package ml.library.core;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import ml.library.core.bean.DataEntry;
import ml.library.utilities.Utilities;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;


public class MLDataGenerator {
	/**
	 * Properties 
	 */
	private static final String DEFAULT_PROPERTIES = "configuration.prop";
	private static final String PROP_SMALL_FILE_NAME= "small.file.path";
	private static final String PROP_BIG_FILE_PATH = "big.file.path";
	private static final String PROP_SAVE_FILE_PATH="save.file.path";
	//private static final String PROP_DELIMETER="save.file.delimeter";
	
	private static final Charset charset = Charset.forName("utf-8");
	
	/*
	 * Local data 
	 */
	private String smallFileName,bigFileName,saveFileName;

	private char delimeter;
	private long current;
	private long previousN=1,nextN=1;
	private DataEntry currentData;
	
	private long startIndex,endIndex,fileLines;//must select the range after analyzing the file
	private String[] smallCategories,bigCategories;
	private Map<Long,DataEntry> forChange;
	
	/*
	 * Graphics
	 */
	private JPanel centerPan;
	private boolean showAllArticle=true;
	private boolean showAllSentence=true;
	private boolean showPrevious=true;
	
	public MLDataGenerator(){
		this(DEFAULT_PROPERTIES);
	}
	public MLDataGenerator(String pathToProperties){
		/*
		 * setting look and feel
		 */
		try{
			UIManager.setLookAndFeel(new NimbusLookAndFeel());
		}catch(Exception e){
			e.printStackTrace();
		}
		/*
		 * Trying to read properties
		 */
		Properties prop = new Properties();
		File propFile = new File(pathToProperties);
		if(!propFile.exists()){ //read user input and write properties file
			
			createPreferencesFromUserInput();
			
			prop.put(PROP_SMALL_FILE_NAME, smallFileName);
			prop.put(PROP_BIG_FILE_PATH, bigFileName);
			prop.put(PROP_SAVE_FILE_PATH,saveFileName);
			
			try(OutputStream out = new FileOutputStream(new File(pathToProperties))){
				prop.store(out, null);
			}catch(Exception e){
				e.printStackTrace();
			}
			
		}else{ //read properties file
			try(InputStream in = Utilities.getInputStream(pathToProperties)){
				prop.load(in);
				String save = prop.getProperty(PROP_SAVE_FILE_PATH);
				if( save == null){
					fail("Missing properties in "+pathToProperties);
				}else{
					this.saveFileName = save;
					this.smallFileName = prop.getProperty(PROP_SMALL_FILE_NAME);
					if(smallFileName == null){
						smallFileName = Utilities.getFile("Select small file path: ");
						if(saveFileName == null)
							fail("small file path generation");
					}
					this.bigFileName = prop.getProperty(PROP_BIG_FILE_PATH);
					if(this.bigFileName == null){
						bigFileName = Utilities.getFile("Select big file path: ");
						if(bigFileName == null)
							fail("big file path generation");
					}
					
					try{
						delimeter ='\t';//prop.getProperty(PROP_DELIMETER).charAt(0);//JOptionPane.showInputDialog("Add Delimeter","\\t").charAt(0);
					}catch(Exception e){
						fail("delimeter");
					}
					readFile();
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
	}
	/**
	 * Always skips headers
	 * @return
	 */
	private CsvReader getReader(){
		return getReader(true);
	}
	private CsvReader getReader(boolean skipHeaders){
		return getReader(saveFileName, skipHeaders);
	}
	/**
	 * Option to skip headers
	 * @param skipHeaders
	 * @return
	 */
	private CsvReader getReader(String filePath, boolean skipHeaders){
		try{
			CsvReader read = new CsvReader(Utilities.getInputStream(filePath), delimeter, charset);
			if(skipHeaders)
				read.readHeaders();
			return read;
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	private String[] readRow(long rowIndex){
		return readRows(rowIndex, rowIndex)[0];
	}
	private String [][] readRows(long from,long to){
		if(from<0){
			from = 0;
		}
		if(to>endIndex ){
			to = endIndex;
		}
		if(from < startIndex || from > endIndex || to > endIndex || to >endIndex){
			return null;
		}
		long howMany = to-from +1;
		try{
			List<String[]> data = new ArrayList<>();
			CsvReader read = getReader(false);
			AtomicLong al = new AtomicLong(0);
			while(read.readRecord()){
				if(data.size() == howMany){
					break;
				}
				if(al.get() >= from && al.get() <= to){
					String[] values = read.getValues();
					if(values.length!=3){
						System.out.println("malformed row - length: "+values.length);
						System.out.println(Arrays.asList(values));
						continue;
					}
					data.add(values);
				}
				al.incrementAndGet();
			}
			read.close();
			System.out.println("read " +data.size());
			return data.toArray(new String[0][]);
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
		
	}
	public DataEntry getCurrentElement(){
		String[] row = readRow(current);
		DataEntry data = new DataEntry(current,
										row.length>=1?row[0]:null,
										row.length>=2?row[1]:null, 
										row.length>=3?row[2]:null);
		return data;
	}
	
	/**
	 * Returns all sentence of the 
	 * @return
	 */
	public List<DataEntry> getCurrentSentence(){
		String punct = "punctuation/--/--/--/--";
		long start = current;
		DataEntry temp = forChange.get(start);
		while(start>=startIndex && !temp.getSmallCategory().equals(punct) && !temp.getWord().equals("%newarticle%")){
			start--;
			temp = forChange.get(start);
		}
		start++;
		long end = current+1;
		temp = forChange.get(end);
		while(end<=endIndex && !temp.getSmallCategory().equals(punct) && !temp.getWord().equals("%newarticle%")){
			end++;
			temp = forChange.get(end);
		}
		//end--;
		//System.out.println(start+" "+end);
		return getCurrentArea(current-start,end-current);
		/**
		 * from %article% or punctuation to next punctuation or article
		 */
	}
	/**
	 * From begin article to end article
	 * @return
	 */
	public List<DataEntry> getCurrentArticle(){
		long start = current;
		DataEntry temp = forChange.get(start);
		while(start>=startIndex && !temp.getWord().equals("%newarticle%")){
			start--;
			temp = forChange.get(start);
		}
		long end = current+1;
		temp = forChange.get(end);
		while(end<=endIndex && !temp.getWord().equals("%newarticle%")){
			end++;
			temp = forChange.get(end);
		}
		end--;
		//System.out.println(start+" "+end);
		return getCurrentArea(current-start,end-current);
	}
	public List<DataEntry> getCurrentArea(long previous,long next){
		long from = current - previous >=startIndex ?current -previous:startIndex;
		long to = current + next <=endIndex ? current+next : endIndex;
		List<DataEntry> list = new ArrayList<>();
		for(long i = from;i<=to;i++){
			list.add(forChange.get(i));
		}
		return list;
	}

	public void refreshPanel(){
		SwingUtilities.invokeLater(()->{
			JPanel pan = new JPanel();
			pan.setOpaque(false);
			pan.setLayout(new BorderLayout());
			JPanel buttonPane = new JPanel(new GridLayout(0,1));
			buttonPane.setOpaque(true);
			buttonPane.setBackground(Color.white);
			JPanel visualizationPanel = new JPanel(new GridLayout(0,1));
			visualizationPanel.setOpaque(false);
			pan.add(visualizationPanel,BorderLayout.CENTER);
			pan.add(buttonPane,BorderLayout.SOUTH);
			final JComboBox<String> smallCategory = new JComboBox<>(smallCategories);
			String currentSmallValue = currentData.getSmallCategory();
			smallCategory.setSelectedItem(currentSmallValue);
			smallCategory.addActionListener((e)-> {
				String smallValue = (String)smallCategory.getSelectedItem();
				if(!currentData.getSmallCategory().equals(smallValue)){
					currentData.setSmallCategory(smallValue);
					currentData.markForWrite();
				}
			});
			JComboBox<String> bigCategory = new JComboBox<>(bigCategories);
			String currentValue = currentData.getBigCategory();
			bigCategory.setSelectedItem(currentValue);
			bigCategory.addActionListener((e)-> {
					String bigValue = (String)bigCategory.getSelectedItem();
					if(!currentData.getBigCategory().equals(bigValue)){
						currentData.setBigCategory(bigValue);
						currentData.markForWrite();
					}
			});
			JPanel selectorPan = new JPanel(new GridLayout(1,2));
			selectorPan.setBackground(Color.white);
			selectorPan.add(smallCategory);
			selectorPan.add(bigCategory);
			if(this.showPrevious){
				JTextPane sentenceDisplay = getRange("Area [-"+previousN+","+nextN+"]", this.getCurrentArea(previousN, nextN));
				visualizationPanel.add(sentenceDisplay);
			}
			if(this.showAllSentence){
				JTextPane sentenceDisplay = getRange("Sentence", getCurrentSentence());
				visualizationPanel.add(sentenceDisplay);
			}
			if(this.showAllArticle){
				JTextPane articleDisplay = getRange("Article", getCurrentArticle());
				visualizationPanel.add(articleDisplay);
			}
			
			JButton next = new JButton("Next");
			JButton next10 = new JButton("Next 10");
			JButton previous = new JButton("Previous");
			JButton previous10 = new JButton("Previous 10");
			previous10.addActionListener((o)->{
				advance(-10);
				refreshPanel();
			});
			next10.addActionListener((i)->{
				advance(+10);
				refreshPanel();
			});
			next.addActionListener((e)->{
				advance(1);
				refreshPanel();
			});
			JTextArea previousText = new JTextArea();
			previousText.setPreferredSize(new Dimension(50,30));
			previousText.setText(""+this.previousN);
			previousText.getDocument().addDocumentListener(new DocumentListener() {
				
				@Override
				public void removeUpdate(DocumentEvent e) { changedUpdate(e); }
				
				@Override
				public void insertUpdate(DocumentEvent e) { changedUpdate(e); }
				
				@Override
				public void changedUpdate(DocumentEvent e) {
					try{
						Long lon = Long.parseLong(previousText.getText());
						if(lon<0){
							throw new Exception("negative");
						}else{
							previousN = lon;
							//refreshPanel();
						}
					}catch(Exception ee){ }
				}
			});
			final JTextArea nextText =new JTextArea();
			nextText.setPreferredSize(new Dimension(50, 30));
			nextText.setText(nextN+"");
			nextText.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void removeUpdate(DocumentEvent e) { changedUpdate(e); }
				
				@Override
				public void insertUpdate(DocumentEvent e) { changedUpdate(e); }
				
				@Override
				public void changedUpdate(DocumentEvent e) {
					try{
						Long lon = Long.parseLong(nextText.getText());
						if(lon<0){
							throw new Exception("negative");
						}else{
							nextN = lon;
							//refreshPanel();
						}
					}catch(Exception ee){
						//SwingUtilities.invokeLater(()->previousText.setText(""+nextN));
					}
				}
			});
			JButton refreshArea = new JButton("Refresh");
			refreshArea.addActionListener((i)->{
				refreshPanel();
			});
			JTextArea jt = new JTextArea();
			jt.setText(""+current);
			jt.setPreferredSize(new Dimension(50,30));
			JButton goToButton = new JButton("Go To:");
			goToButton.addActionListener((l)->{
				try{
					Long lon = Long.parseLong(jt.getText());
					advance((int)(lon-current));
					refreshPanel();
				}catch(Exception e){
					jt.setText(currentValue+"");
				}
			});
			
			JLabel currentLab = new JLabel("Word: "+current+" "+currentData.getWord()+" --> "+currentData);
			currentLab.setBackground(Color.white);
			previous.addActionListener((e)->{
				advance(-1);
				refreshPanel();
			});
			buttonPane.setPreferredSize(new Dimension(1, 200));
			JPanel buttPan = new JPanel(new FlowLayout());
			//buttPan.setBackground(Color.white);
			buttPan.add(refreshArea);
			buttPan.add(previousText);
			buttPan.add(nextText);
			buttPan.add(new JSeparator(JSeparator.VERTICAL));
			buttPan.add(previous10);
			buttPan.add(previous);
			buttPan.add(next);
			buttPan.add(next10);
			buttPan.add(new JSeparator(JSeparator.VERTICAL));
			buttPan.add(goToButton);
			buttPan.add(jt);
			buttonPane.add(currentLab);
			buttonPane.add(selectorPan);
			buttonPane.add(buttPan);
			buttonPane.validate();
			buttonPane.repaint();
			replacePanel(pan);
		});
	}
	public JTextPane getRange(String title,List<DataEntry> data){
		JTextPane articleDisplay = new JTextPane();
		articleDisplay.setBorder(new TitledBorder(title));
		articleDisplay.setEditable(false);
        StyledDocument doc = articleDisplay.getStyledDocument();
        Style redStyle = articleDisplay.addStyle("Red Style", null);
        StyleConstants.setBackground(redStyle, Color.red);
        Style blueStyle = articleDisplay.addStyle("Black Style",null);
        StyleConstants.setBackground(blueStyle, Color.white);
        List<DataEntry> article = data;
		article.forEach(i->{
			try { 
				doc.insertString(doc.getLength(), i.getWord()+" ",i.getRowIndex().equals(currentData.getRowIndex())?redStyle:blueStyle);
			}catch (Exception e){
        		e.printStackTrace();
        	}
		});
		return articleDisplay;
	}
	public void advance(int delta){
		this.current+=delta;
		if(this.current<startIndex){
			this.current=startIndex;
		}
		if(this.current>endIndex){
			this.current=endIndex;
		}
		this.currentData = forChange.get(this.current);
	}
	public void replacePanel(Component newPanel){
		centerPan.invalidate();
		centerPan.removeAll();
		centerPan.add(newPanel,BorderLayout.CENTER);
		centerPan.validate();
		centerPan.repaint();
	}
	public void readFile() throws Exception{
		this.forChange = new LinkedHashMap<>();
		AtomicLong al = new AtomicLong();
		CsvReader read = getReader();
		Set<String> small = new HashSet<>();
		CsvReader readSmall = getReader(smallFileName, false);
		while(readSmall.readRecord()){
			small.add(readSmall.getValues()[0]);
		}
		this.smallCategories = small.toArray(new String[0]);
		Arrays.sort(smallCategories);
		readSmall.close();
		Set<String> big = new HashSet<>();
		CsvReader readBig = getReader(bigFileName,false);
		while(readBig.readRecord()){
			big.add(readBig.getValues()[0]);
		}
		this.bigCategories = big.toArray(new String[0]);
		Arrays.sort(bigCategories);
		while(read.readRecord()){
			al.incrementAndGet();
		}
		read.close();
		this.fileLines = al.get();
		selectFileRangeToProcess((int)fileLines);
		al.set(startIndex);
		for(String[] arr : readRows(startIndex, endIndex)){
			forChange.put(al.get(), new DataEntry(al.get(), arr[0], arr[1], arr[2]));
			al.incrementAndGet();
		}
		/*System.out.println(forChange.get(startIndex));
		System.out.println(forChange.get(endIndex));*/
	}
	public static void fail(String location){
		System.out.println(location);
		System.exit(0);
	}
	public void createPreferencesFromUserInput(){
		String pathForSmall = Utilities.getFile("Enter Location for small properties file:");
		if(pathForSmall!=null && new File(pathForSmall).exists()){
			this.smallFileName = pathForSmall;
		}else{
			fail("Small file");
		}
		String pathForBig = Utilities.getFile("Enter location for big properties file:");
		if(pathForBig !=null && new File(pathForBig).exists()){
			this.bigFileName = pathForBig;
		}else{
			fail("Big prop file");
		}
		String pathToFile = Utilities.getFile("Select file location of data");
		if(pathToFile == null){
			fail("No save file selected");
		}
		this.saveFileName = pathToFile;
		try{
			delimeter = JOptionPane.showInputDialog("Enter Delimeter","\t").charAt(0);
		}catch(Exception e){
			fail("Delimeter arch");
		}
	}
	public void selectFileRangeToProcess(int max){
		final JSlider slide  = new JSlider(0,(int) max,0);
		final JSlider slide2 = new JSlider(0,(int)max,(int) max);
		final JLabel lab1 = new JLabel("From :"+slide.getValue());
		final JLabel lab2 = new JLabel("To: "+slide2.getValue());
		slide.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				if(slide.getValue()>slide2.getValue())
					slide.setValue(slide2.getValue());
				lab1.setText("From :"+slide.getValue());
			
		}});
		slide2.addChangeListener( new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				if(slide2.getValue()<slide.getValue()){
					slide2.setValue(slide.getValue());
				}
				lab2.setText("To: "+slide2.getValue());
			}
		});
		JPanel root = new JPanel(new GridLayout(2, 2));
		root.add(lab1);
		root.add(lab2);
		root.add(slide);
		root.add(slide2);
		
		Utilities.displayIntoDialog(root, "Select File Range: ");
		
		this.startIndex = slide.getValue();
		this.current = startIndex;
		this.endIndex = slide2.getValue();
		
	}
	/**
	 * Save changes in files
	 */
	public void saveChanges(){
		
		long counter = this.forChange.values().stream().filter(DataEntry::isMarked).count();
		if(counter<=0)
			fail("Normal Exit");
		
		try(OutputStream faos = new FileOutputStream(new File(this.saveFileName+".temp"))){
			CsvWriter csvw = new CsvWriter(faos, delimeter, charset);
			CsvReader read = getReader(false);
			AtomicLong al = new AtomicLong(0);
			long counterEdit = 0;
			while(read.readRecord()){
				String[] record = read.getValues();
				DataEntry de = forChange.get(al.get());
				if(de==null){
					break;
				}
				if(de.isMarked()){
					de.setMarked(false);
					counterEdit++;
					record = new String[]{
							de.getWord(),
							de.getSmallCategory(),
							de.getBigCategory()
					};
				}
				al.incrementAndGet();
				csvw.writeRecord(record);
			}
			read.close();
			csvw.close();
			System.out.println("Edited "+counterEdit+" rows");
			new File(this.saveFileName).delete();
			new File(this.saveFileName+".temp").renameTo(new File(this.saveFileName));
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void promptExit(){
		long counter = this.forChange.values().stream().filter(i->i.isMarked()).count();
		if(counter!=0){
			if(JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null, "Are you sure you want to discard "+counter+" changes?")){
				fail("Exit without saving");
			}else{
				saveChanges();
				fail("All saved");
			}
		}else{
			fail("Normal Exit");
		}
	}
	public void start(){
		//TODO 
		SwingUtilities.invokeLater(()->{
			JFrame mainFrame = new JFrame("ML Data Generator");
			double screenWidth = Toolkit.getDefaultToolkit().getScreenSize().getWidth();
			double screenHeight = Toolkit.getDefaultToolkit().getScreenSize().getHeight();
			screenWidth*=0.3;
			screenHeight*=0.5;
			mainFrame.setSize((int)screenWidth,(int) screenHeight);
			mainFrame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent we ){
					promptExit();
				}
			});
			mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			mainFrame.setLocationRelativeTo(null);
			/*
			 * main panel of the project
			 */
			centerPan = new JPanel(new BorderLayout());
			centerPan.setBackground(Color.white);
			
			JMenu fileMenu = new JMenu("File");
			JMenuItem saveItem = new JMenuItem("Save..");
			saveItem.addActionListener(i->saveChanges());
			fileMenu.add(saveItem);
			JMenuItem exitItem = new JMenuItem("Exit");
			exitItem.addActionListener(i->promptExit());
			fileMenu.add(exitItem);
			
			JMenu view = new JMenu("View");
			JCheckBoxMenuItem jc1 = new JCheckBoxMenuItem("Show Sentence");
			jc1.setSelected(showAllSentence);
			jc1.addActionListener((l)->{
				showAllSentence = !showAllSentence;
				refreshPanel();
			});
			JCheckBoxMenuItem jc2 = new JCheckBoxMenuItem("Show Paragraph");
			jc2.setSelected(showAllArticle);
			jc2.addActionListener((l)->{
				showAllArticle = !showAllArticle;
				refreshPanel();
			});
			JCheckBoxMenuItem jc3 = new JCheckBoxMenuItem("Show Range");
			jc3.setSelected(showPrevious);
			jc3.addActionListener((l)->{
				showPrevious = !showPrevious;
				refreshPanel();
			});
			view.add(jc1);
			view.add(jc2);
			view.add(jc3);
			JMenuBar jmb = new JMenuBar();
			jmb.add(fileMenu);
			jmb.add(view);
			mainFrame.setJMenuBar(jmb);
			exitItem.addActionListener(i->promptExit());
			centerPan.setOpaque(false);
			mainFrame.getContentPane().add(centerPan, BorderLayout.CENTER);
			mainFrame.getContentPane().validate();
			mainFrame.getContentPane().repaint();
			advance(0);
			refreshPanel();
			mainFrame.setVisible(true);
		});
	}
	public static void main(String... args){
		if(args.length==1){
			new MLDataGenerator(args[0]).start();
		}else{
			new MLDataGenerator().start();
		}
	}
}
