package com.applitools.batchmapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;

import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;
import technology.tabula.extractors.BasicExtractionAlgorithm;

/**
 * 
 */

/**
 * @author levv
 *
 */
public class MFSBatchMapper {
	
	public static int OFFSET = 2;
   

	public static void main(String[] args) throws IOException {
		    
		     if( args.length < 2 )
		        {
		            usage();
		        }
		        else
		        {
		        	MFSBatchMapper batchMapper = new MFSBatchMapper();
		        	batchMapper.execute(args);
		        }  
		  
		}
	  	  
	    /**
	     * Print help.
	     */
	    private static void usage()
	    {
	        System.err.println( "Usage: java -jar MFSBatchMapper.jar -f <A path to target folder or file>" );
	    }

	   
	  public void execute(String[] args) throws IOException
	  {
		  String path = "";
		  
		  for (int i=0; i < args.length; i++) {
	            if (args[i].equals("-f")) {
	                i++;
	                path = args[i];
	            }
	        }
		  
		  if(path == "") 
		  {
			  usage();
			  return;
		  }
		    
		  File root = new File(path);
		  	if (root.isFile())
		  	{
		  		parseFile(root);
		  	}
		  	else
		  	{
		  		File[] files = root.listFiles();
		  	   for (File file : files) {
		  		 parseFile(file);
	            }
		  	}
	  }
	  
	  public void parseFile(File file) throws IOException
	  {
		  ArrayList<String> TOC_Text = new ArrayList<String>();
		  ArrayList<Integer> TOC_Pages = new ArrayList<Integer>();
		    
		    PDDocument pd = PDDocument.load(file);

		    int totalPages = pd.getNumberOfPages();
//		    System.out.println("Total Pages in Document: "+totalPages);

		    ObjectExtractor oe = new ObjectExtractor(pd);
		    BasicExtractionAlgorithm sea = new BasicExtractionAlgorithm();
		    Page page = oe.extract(2);

		    // extract text from the table after detecting
		    List<Table> table = sea.extract(page);
		    for(Table tables: table) {
		        List<List<RectangularTextContainer>> rows = tables.getRows();

		        for(int i=0; i<rows.size(); i++) {

		            List<RectangularTextContainer> cells = rows.get(i);

		            for(int j=0; j<cells.size(); j++) {
		            	String text = cells.get(j).getText();
		            	//System.out.println(text);
		            	
		            	if(Pattern.matches("[a-zA-Z ]+\\d+$", text) && text.length()>1)
		            	{
		            	try {
		            		int len=text.length();
		            		TOC_Text.add(text.substring(0, text.lastIndexOf(" ")==-1? text.length()-1 :text.lastIndexOf(" ")));
          			            		
		            		Pattern numbers = Pattern.compile("\\d+");
		            		Matcher matcher = numbers.matcher(text);
		            		if (matcher.find())
		            		{
		            		    TOC_Pages.add(Integer.parseInt(matcher.group(0)));
		            		}
		            		
		            	}
		            	catch (Exception e) {
							// TODO: handle exception
		            		System.out.println(e.getMessage());
						}
	       
		            		            		
		            	}
		            	
		     
	            	
		            }

		        }
		    }
		    pd.close();
		    generateIT(TOC_Text,CalcPages(TOC_Pages,totalPages),file);

	  }
	  
	  
	  
	  
	  public ArrayList<String> CalcPages(ArrayList<Integer> pages,int totalPages)
	  {
		    ArrayList<String> TOC_pages = new ArrayList<String>();
		    int loopSize= pages.size()-1;
		    int i,a=0,b;
		    	
		    for(i=0;i<loopSize;i++)
		    {
		    	a=pages.get(i);
		    	b=pages.get(i+1);
		    	
		    	if(b-a==1)
		    	{
		    		TOC_pages.add(i, ""+a);
		    	}
		    	else
		    		TOC_pages.add(i, String.format("%d-%d", a,b-1));
		    }

		    totalPages=totalPages-OFFSET;
		    a=pages.get(i);
		    
	    	if(totalPages-a==0)
	    	{
	    		TOC_pages.add(i, ""+a);
	    	}
	    	else
	    		TOC_pages.add(i, String.format("%d-%d", a,totalPages));
		   
	   return TOC_pages;
		  
	  }
	  
	  public void generateIT(ArrayList<String> text, ArrayList<String> pages,File file) throws IOException
	  {
		  int i;
		  
		  for(i=0;i<text.size();i++)
		  {
			  String str = "java -jar ~/Documents/ImageTester_2.0.0.jar -f %s -k fcq4rttvnfdjwWt6v99c8cC6FomulWtHwxz3fn104kPf6o110 -a %s -fn \"%s\" -sp %s -fb \"TOC1<>MAP1\" &&";
			  str=String.format(str,file.getCanonicalPath(),file.getName(), text.get(i),pages.get(i));
			  
			  System.out.println(str);
			 try {
				Runtime.getRuntime().exec(str);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		  }
		  
	  }
	  
}
