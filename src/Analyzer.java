import it.uniroma3.mat.extendedset.intset.ConciseSet;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import org.roaringbitmap.IntIterator;
import org.roaringbitmap.ShortIterator;
import org.roaringbitmap.buffer.ImmutableRoaringBitmap;
import org.roaringbitmap.buffer.MappeableArrayContainer;
import org.roaringbitmap.buffer.MappeableContainerPointer;


public class Analyzer {

	public static void main(String[] args) {
		try {
			final String[] dimensions = {"dimension_003","dimension_008","dimension_033"};
			File dir = new File("bitmaps");
			
			for(int i=0; i<dimensions.length; i++) {
				final String dimName = dimensions[i];
				PrintWriter writer = new PrintWriter("results\\"+dimName+"Analysis.txt","UTF-8");
				File[] dimFiles = dir.listFiles(new FileFilter(){
					public boolean accept(File pathname) {
						if(pathname.getName().contains(dimName))
							return true;
						return false;
					}					
				});
				int maxCardinality = 0, avCardinality = 0, nbArrayContainers=0, nbBitmapContainers=0, nbIntsInArrays=0, nbIntsInBitmaps=0, totConciseWords=0, 
						arraysByteLength=0, totIntsInInter=0, totNbInter=0, totRBsizeInBytes=0, totCardinality=0;
				
				for(File dimFile : dimFiles) {
					RandomAccessFile memoryMappedFile = new RandomAccessFile(dimFile, "r");
					MappedByteBuffer mbb = memoryMappedFile.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, memoryMappedFile.length());
			
					ImmutableRoaringBitmap irb = new ImmutableRoaringBitmap(mbb);
					int cardinality = irb.getCardinality();
					totCardinality+=cardinality;
					totRBsizeInBytes += irb.getSizeInBytes();
					writer.println("#########################################################################################");
					writer.println(dimFile.getName()+" cardinality = "+cardinality+"\nIRB sizeInBytes = "+irb.getSizeInBytes());
					if(irb.getCardinality()>maxCardinality)
						maxCardinality=cardinality;
					avCardinality+=cardinality;
					//Printing the bitmap's integers
					ConciseSet cs = new ConciseSet();
					writer.println("Printing integers :");
					IntIterator it = irb.getIntIterator();
					while(it.hasNext()){
						int x = it.next();
						cs.add(x);
						writer.print(x+", ");
					}
					writer.println();
					
					//Printing the containers
					MappeableContainerPointer mc = irb.showContainers();
					writer.println("Printing the containers :");
					int cpContainers = 0;
					while(mc.hasContainer()) {
						ArrayList<String> intervals = new ArrayList<String>();
						ShortIterator shIt = mc.getContainer().getShortIterator();
						String containerType = (mc.getContainer() instanceof MappeableArrayContainer) ? "array" : "bitmap";
						
						writer.println("Container n°"+(cpContainers++)+" is a "+containerType+" :");
						boolean suite= false; 
						int debInter = 65536, finInter = -65536, nbInter=0, prev=-70000, nbValInter=1, nbInts=0;
						//Printing container's short integers
						while(shIt.hasNext()) {
							int currentInt = shIt.next();
							nbInts++;
							writer.print(" "+currentInt+",");
							if(currentInt==(prev+1)){
								suite=true;
								nbValInter++;
								if(prev<debInter)
									debInter=prev;
								if(currentInt>finInter)
									finInter=currentInt;
							}
							else if(suite==true){//a suite has just finished
									suite=false;
									nbInter++;
									intervals.add("["+debInter+", "+finInter+"],-->"+nbValInter);
									totIntsInInter+=nbValInter;
									nbValInter=1;
									debInter=65536;
									finInter=-65536;
								  }
							prev=currentInt;
						}
						if(suite==true){//The last ints succession forms a suite
							nbInter++;
							intervals.add("["+debInter+", "+finInter+"],-->"+nbValInter);
							totIntsInInter+=nbValInter;
						}
						writer.println();
						
						if (containerType == "array"){ 
							nbIntsInArrays+=nbInts;
							nbArrayContainers++;
							arraysByteLength+=mc.getContainer().getSizeInBytes();
						}
						else {
							nbIntsInBitmaps+=nbInts;
							nbBitmapContainers++;
						}
						totNbInter+=nbInter;
						//printing the intervals
						writer.println("The container's intervals ([begin, end]-->nbValues) == "+nbInter);
						writer.println(intervals.toString());
						mc.advance();
					}					
					int[] csWords = cs.getWords();
					totConciseWords+=csWords.length;
					writer.println("Immutable ConciseSet sizeInBytes = "+(csWords.length*4)+", nb words = "+csWords.length);
				}				
				writer.close();
				System.out.println(dimName+"_Max cardinality = "+maxCardinality);
				System.out.println(dimName+"_Average cardinality = "+avCardinality/dimFiles.length);
				System.out.println(dimName+"_Average length of a positif bits sequence = "+totIntsInInter/totNbInter);
				System.out.println("nbArrayContainers = "+nbArrayContainers+", nbIntsInArrays = "+nbIntsInArrays+" ---> size = "
									+arraysByteLength+" bytes = "+(arraysByteLength*8/nbIntsInArrays)+" bits/int");
				System.out.println("nbBitmapContainers = "+nbBitmapContainers+", nbIntsInBitmaps = "+nbIntsInBitmaps+" ---> size = "
									+(8192*nbBitmapContainers)+" bytes = "+(65536*nbBitmapContainers/nbIntsInBitmaps)+" bits/int");
				//System.out.println("Check if totCardinality = "+totCardinality+", nbInBitmaps+nbInArrays = "+nbIntsInBitmaps);
				System.out.println("Total Roaring size = "+totRBsizeInBytes+" bytes = "+((totRBsizeInBytes*8)/totCardinality)+" bits/int");
				System.out.println("Total Concise size = "+(totConciseWords*4)+" bytes = "+
									+(float)(totConciseWords*32)/(totCardinality)+" bits/int");
				
				System.out.println("#################################################################################");
			}
			
			System.out.println("\nFor more detailed analysis results, please see the files in the result directory under the project's root directory");
			
		} catch (IOException e) {e.printStackTrace();}
	}
}
