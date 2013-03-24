/*        __
 *        \ \
 *   _   _ \ \  ______
 *  | | | | > \(  __  )
 *  | |_| |/ ^ \| || |
 *  | ._,_/_/ \_\_||_|
 *  | |
 *  |_|
 * 
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE" (Revision 42):
 * <rob ∂ CLABS dot CC> wrote this file. As long as you retain this notice you
 * can do whatever you want with this stuff. If we meet some day, and you think
 * this stuff is worth it, you can buy me a beer in return.
 * ----------------------------------------------------------------------------
 */
package cc.clabs.stratosphere.mlp.contracts;

import cc.clabs.stratosphere.mlp.types.PactIdentifiers;
import cc.clabs.stratosphere.mlp.types.PactRelation;
import cc.clabs.stratosphere.mlp.types.PactSentence;
import cc.clabs.stratosphere.mlp.types.PactWord;
import eu.stratosphere.nephele.configuration.Configuration;
import eu.stratosphere.pact.common.stubs.CoGroupStub;
import eu.stratosphere.pact.common.stubs.Collector;
import eu.stratosphere.pact.common.type.PactRecord;
import eu.stratosphere.pact.common.type.base.PactDouble;
import eu.stratosphere.pact.common.type.base.PactInteger;
import eu.stratosphere.pact.common.type.base.PactString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author rob
 */
public class CandidateEmitter extends CoGroupStub {
        
    private static final Log LOG = LogFactory.getLog( CandidateEmitter.class );
    
    private ArrayList<String> identifiers = new ArrayList<>();
    
    private PactInteger document = null;
    
    private final static List<String> blacklist = Arrays.asList(
        "function", "functions",
        "behavior", "infinity", "sum", "other", "=", "|",
        "equation", "equations",
        "value", "values",
        "solution", "solutions",
        "result", "results"
    );
    
    private Double α;
    private Double β;
    private Double γ;
    
    @Override
    public void open(Configuration parameter) throws Exception {
      super.open( parameter );
      α = Double.parseDouble( parameter.getString( "α", "1" ) );
      β  = Double.parseDouble( parameter.getString( "β", "1" ) );
      γ = Double.parseDouble( parameter.getString( "γ", "1" ) );
    }

    
    @Override
    public void coGroup( Iterator<PactRecord> left, Iterator<PactRecord> right, Collector<PactRecord> collector ) {
        
        // populating identifier list
        // we'l allways get one record from the left,
        // therefore, we don't need to iterate through
        // left
        Iterator<PactString> it = left.next().getField( 2, PactIdentifiers.class ).iterator();
        while ( it.hasNext() ) identifiers.add( it.next().getValue() );
        

        // populating sentences list
        ArrayList<PactSentence> sentences =  new ArrayList<>();
        while ( right.hasNext() ) {
            PactRecord next = right.next();
            // document should always be the same
            document = next.getField( 0, PactInteger.class );
            // we need to clone the sentence objects, because of reused objects
            sentences.add( (PactSentence) next.getField( 1, PactSentence.class ).clone() );
        }
        
        PactRecord output = new PactRecord();
        
        for ( String identifier : identifiers ) {
            ArrayList<PactSentence> list = new ArrayList<>();
            // populate the list
            for ( PactSentence sentence : sentences )
                if ( sentence.containsWord( identifier ) )
                    list.add( sentence );
            // emit the generated candidate sentences
            for ( PactRecord candidate : generateCandidates( list, identifier ) )
                collector.collect( candidate );
        }
        
    }
    
    
    /**
     * 
     * @param sentences
     * @param identifier
     * @return 
     */
    private ArrayList<PactRecord> generateCandidates( ArrayList<PactSentence> sentences, String identifier ) {
        ArrayList<PactRecord> candidates = new ArrayList<>();
        HashMap<String,Integer> ω = new HashMap<>();
        Integer Ω = 0;
        
        /*                         _
         *                        / |
         *   ____ ___ ___ ______  - |
         *  /  ._|   ) __|  __  ) | |
         * ( () ) | |> _) | || |  | |
         *  \__/   \_)___)|_||_|  |_|
         * calculate the word frequencies for sentences
         */
        for ( PactSentence sentence : sentences ) {
            for ( PactWord word : sentence ) {
                // only count words we're interested in
                if ( filterWord( word ) ) continue;
                Integer count = ( ω.containsKey( word.getWord() ) ) ?
                        ω.get( word.getWord() ) + 1 : 1;
                // update the maximun token frequency
                Ω = Math.max( count, Ω );
                ω.put( word.getWord(), count );
            }            
        }
                
        /*                        ____
         *                       (___ \
         *  ____ ___ ___ ______    __) )
         * /  ._|   ) __|  __  )  / __/
         *( () ) | |> _) | || |  | |___
         * \__/   \_)___)|_||_|  |_____)
         * the kernel step
         */
        Integer index = -1; // will be zero on the first loop
        for ( Iterator<PactSentence> it = sentences.iterator(); it.hasNext(); ) {
            index += 1;
            PactSentence sentence = it.next();
            
            ArrayList<Integer> positions = sentence.getWordPosition( identifier );
            
            Integer position = -1; // will be zero on the first loop
            for ( PactWord word : sentence ) {
                position += 1;
                if ( filterWord( word ) ) continue;
                
                Integer Δ = getMinimumDistance( position, positions );
                Double score = getScore( Δ, ω.get( word.getWord() ), Ω, index );
                
                // create a relation object
                PactRelation relation = new PactRelation();
                relation.setScore( score );
                relation.setIdentifier( identifier );
                relation.setWordPosition( position );
                relation.setIdentifierPosition( position + Δ );
                relation.setSentence( sentence );
                
                // emit the relation            
                PactRecord record = new PactRecord();
                record.setField( 0, document );
                record.setField( 1, relation );
                candidates.add( record );
            }
        }
        return candidates;
    }
    
    
    /**
     * 
     * @param pos
     * @param positions
     * @return 
     */
    private Integer getMinimumDistance( Integer pos, ArrayList<Integer> positions ) {
        Integer min = Integer.MAX_VALUE;
        for ( Integer position : positions )
            min = Math.min( min, position - pos );
        return min;
    }
    
    
    /**
     * 
     * @param Δ
     * @param ω
     * @param Ω
     * @param x
     * @return 
     */
    private Double getScore( Integer Δ, Integer ω, Integer Ω, Integer x ) {        
        Double dist = gaussian( (double) Δ, 5d / Math.sqrt( 2 * Math.log( 2 ) ) );
        Double seq = gaussian( (double) x, 3d / Math.sqrt( 2 * Math.log( 2 ) ) );
        Double freq = (double) ω / (double) Ω;
        return ( α * dist + β * seq + γ * freq ) / ( α + β + γ );
    }
    
    
    /**
     * Returns the value of the gaussian function
     * at x. C is a real constant. One can control
     * how steep the curve will fall down by choosing
     * lower values of C.
     * 
     * @param x
     * @param C
     * @return 
     */
    private Double gaussian( Double x, Double C ) {
        return Math.exp( - Math.pow( x, 2d ) /
               ( 2d * Math.pow( 2d * C, 2d ) ) );
    }

     
    /**
     *
     * @param word
     * @param identifier
     * @return 
     */
    private boolean filterWord( PactWord word ) {
               // skip the identifier words
        return identifiers.contains( word.getWord() ) ||
               // skip blacklisted words
               blacklist.contains( word.getWord() ) ||
               // we're only interested in nouns, adjectives and entities
               !word.getTag().matches( "NN[PS]{0,2}|ENTITY|JJ" );
    }
    
}