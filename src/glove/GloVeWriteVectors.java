package glove;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.jblas.DoubleMatrix;

import glove.objects.Cooccurrence;
import glove.objects.Vocabulary;
import glove.utils.Options;

/**
 * Read raw_text.txt --> Generate Vectors Matrix and save to txt File 
 * @author jason
 *
 */
public class GloVeWriteVectors {
	
	public static void main(String[] args) throws IOException {
		String file = "raw_text.txt";
        
        Options options = new Options(); 
        options.debug = true;
        
        Vocabulary vocab = GloVe.build_vocabulary(file, options);
        
        options.window_size = 3; 
        
        List<Cooccurrence> c =  GloVe.build_cooccurrence(vocab, file, options);
        
        options.iterations = 1;
        options.vector_size = 100;
        options.debug = true;

        DoubleMatrix W = GloVe.train(vocab, c, options);
        
        PrintWriter out = new PrintWriter("vectors_matrix.txt");
     
        out.println(W.toString().replace("[","").replace("]", "").replace(", ", " ").replace(",", "."));
        out.close();
	}
}
