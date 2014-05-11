package learning;

import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

//ds custom imports
import utility.CLogger;
import utility.CMySQLManager;
import utility.CPattern;
import utility.CTag;
import exceptions.CZEPEoIException;
import exceptions.CZEPMySQLManagerException;
import exceptions.CZEPnpIException;

public final class CLearnerBayes
{
    //ds enums
    public static enum ELearnerLabel{ LIKE, DISLIKE }

    //ds data pool control elements
    private final int m_iSizeSelection   = 10;
    private int m_iIndexSelection        = 0;
    private int m_iIndexPrevious         = 0;
    private int m_iCounterClassification = 0;
    
    //ds pools
    private Vector< CPattern > m_vecDataPool          = null; //ds contains all data at start (SHRINKING for each classification)
    private Vector< CPattern > m_vecSelectionPool     = null; //ds contains all data selected for picks (GROWING for each classification)
    private Vector< CPattern > m_vecSelectionPoolNext = null; //ds contains next picks (CONSTANT)
    
    //ds random sample frequency - every n times (for 1 there is no classification done forever)
    private final int m_iIntervalRandomSample  = 1;
    private final int m_iNumberOfRandomSamples = 1;
    private boolean m_bWasPreviousRequested    = false;
    
    //ds simple communication with main
    private boolean m_bIsReadyToClassify = false;
    private boolean m_bIsClassifying     = false;

    //ds absolute probabilities map: [id][value] (e.g. liked has id: -4, a tag has always the same id as id_tag)
    Map< Integer, Double > m_mapAbsoluteProbabilities = null;
    
    //ds cutoff frequency
    final int m_iTagCutoffFrequency = 10;

    //ds counters 
    private int m_iCounterLikes         = 0;
    private int m_iCounterDislikes      = 0;
    //private int m_iCounterLikesAnimated = 0; private int m_iCounterLikesNotAnimated = 0; TODO GIFS
    private int m_iCounterLikesPhoto    = 0; private int m_iCounterLikesNotPhoto    = 0;
    private int m_iCounterLikesText     = 0; private int m_iCounterLikesNotText     = 0;
    private int m_iCounterLikesLiked    = 0; private int m_iCounterLikesNotLiked    = 0;
    private int m_iCounterLikesHot      = 0; private int m_iCounterLikesNotHot      = 0;
    
    //ds counter for different tags: [id][counter] - gets initialized with 0
    private Map< Integer, Integer > m_mapCounterLikesTag = null;
    
    //ds vector with complete pick history
    private Vector< Integer > m_vecIDsHistory = new Vector< Integer >( 0 );
    
    //ds statistics: this vector keeps track of the LIKE/DISLIKE/.. sequence format: [id_datapoint][label]
    private Map< Integer, ELearnerLabel > m_mapLabels  = new HashMap< Integer, ELearnerLabel >( 0 );
    
    //ds mysql manager
    private final CMySQLManager m_cMySQLManager;
    
    //ds username
    private String m_strUsername = null;
    
    //ds total numbers
    private int m_iNumberOfPatterns                   = 0;
    private Map< Integer, Integer > m_mapNumbersOfTag = null;
    
    //ds constructor
    public CLearnerBayes( final CMySQLManager p_cMySQLManager )
    {        
        //ds setup the manager
        m_cMySQLManager = p_cMySQLManager;
        
        System.out.println( "[" + CLogger.getStamp( ) + "]<CLearnerBayes>(CLearnerBayes) Instance allocated" );
    }
    
    //ds launcher
    public void launch( ) throws CZEPMySQLManagerException, CZEPEoIException, SQLException
    {
        //ds check if empty
        if( m_cMySQLManager.isEmpty( ) )
        {
            //ds info
            System.out.println( "[" + CLogger.getStamp( ) + "]<CLearnerBayes>(launch) MySQL table is empty - fatal" );
            
            //ds escape
            throw new CZEPEoIException( "empty database" ); 
        }
        
        //ds get probabilities
        m_mapAbsoluteProbabilities = m_cMySQLManager.getProbabilityMap( m_iTagCutoffFrequency );
        
        //ds get the available tag ids
        final Vector< Integer > vecTagIDs = new Vector< Integer >( m_cMySQLManager.getTagIDs( m_iTagCutoffFrequency ) );
        
        //ds allocate counters map
        m_mapCounterLikesTag = new HashMap< Integer, Integer >( vecTagIDs.size( ) );
        
        //ds add all the ids to our hashmap
        for( Integer iID: vecTagIDs )
        {
            //ds set the current counter to 0
            m_mapCounterLikesTag.put( iID, 0 );
        }
        
        //ds get number of patterns and map with tag numbers
        m_iNumberOfPatterns = m_cMySQLManager.getNumberOfPatterns( m_iTagCutoffFrequency );
        m_mapNumbersOfTag   = m_cMySQLManager.getNumbersOfTag( m_iTagCutoffFrequency );
        
        //ds check consistency
        if( m_mapCounterLikesTag.size( ) != m_mapNumbersOfTag.size( ) )
        {
            //ds escape
            throw new CZEPEoIException( "invalid fetching from database" );             
        }
    }
    
    //ds simple getter - does not change the request counter for an update cycle
    public final CPattern getFirstDataPoint( )
    {
        //ds register pick
        m_vecIDsHistory.add( m_vecSelectionPool.get( 0 ).getID( ) );
        
        //ds retrieve the first datapoint
        return m_vecSelectionPool.get( 0 );
    }
    
    //ds returns the next image
    public final CPattern getNextPattern( final ELearnerLabel p_eFlag, final CPattern p_cLastPattern ) throws SQLException
    {
        //ds check if we land here after a previous call - which means we have to undo the settings for that pattern
        if( m_bWasPreviousRequested )
        {
            //ds undo the pick for the previous pattern before registering the new settings
            _undoPick( p_cLastPattern );
            
            //ds update log
            m_cMySQLManager.logUpdatePattern( m_strUsername, p_cLastPattern, ELearnerLabel.LIKE == p_eFlag );
            
            //ds evaluate the pick again
            _evaluatePick( p_cLastPattern, p_eFlag );
            
            //ds disable previous phase
            m_bWasPreviousRequested = false;
            
            //ds decrease pick counter by one, we want to see the image again at the previous start
            --m_iIndexSelection;
        }
        else
        {
            //ds log to learner
            m_cMySQLManager.logAddPattern( m_strUsername, p_cLastPattern, ELearnerLabel.LIKE == p_eFlag );
            
            //ds evaluate the pick normally
            _evaluatePick( p_cLastPattern, p_eFlag );
        }
        
        //ds increase pick counter
        ++m_iIndexSelection;
        
        //ds check if we have to classify
        if( 0 == m_iIndexSelection%m_iSizeSelection )
        {
            //ds check if main is still busy from the last request
            if( m_bIsClassifying )
            {
                System.out.println( "[" + CLogger.getStamp( ) + "]<CLearnerBayes>(getNextDataPoint) Received classification request but main is busy" );                
            }
            
            //ds communicate with main over booleans we just have to set the ready boolean and go on
            m_bIsReadyToClassify = true;
            
            //ds update selection pool
            m_vecSelectionPool.addAll( new Vector< CPattern >( m_vecSelectionPoolNext ) );
            
            //ds clear next
            m_vecSelectionPoolNext.clear( );
        }
        
        //ds and retrieve the next datapoint and return it
        return m_vecSelectionPool.get( m_iIndexSelection );
    }
    
    //ds returns the previous image - does not change the request counter for an update cycle
    public final CPattern getPreviousPattern( ) throws CZEPnpIException
    {
        //ds check if its the first previous click
        if( !m_bWasPreviousRequested )
        {
            //ds update previous index - selection index stays at the front
            m_iIndexPrevious = m_iIndexSelection-1;
            
            //ds set phase start
            m_bWasPreviousRequested = true;
        }
        else
        {
            //ds go back further
            m_iIndexPrevious = m_iIndexPrevious-1;
        }
        
        //ds check if we would access an image before the start
        if( 0 > m_iIndexPrevious )
        {
            //ds no previous picture available
            throw new CZEPnpIException( "No previous image available" );
        }
        
        //ds return the pattern
        return m_vecSelectionPool.get( m_iIndexPrevious );
    }
    
    //ds sets counters for a like/dislike
    public final void _evaluatePick( final CPattern p_cPattern, final ELearnerLabel p_eLabel  ) throws SQLException
    {
        //ds get the id
        final int iIDCurrentPattern = p_cPattern.getID( );
        
        //ds save the flag for the last image
        m_mapLabels.put( iIDCurrentPattern, p_eLabel );
        
        //ds check if LIKE or DISLIKE
        if( ELearnerLabel.LIKE == p_eLabel )
        {
            //ds increase counter
            ++m_iCounterLikes;
            
            //ds specifics TODO GIFS
            //if( p_cLastPattern.isAnimated( ) ){ ++m_iCounterLikesAnimated; }
            //else                              { ++m_iCounterLikesNotAnimated; }
            if( p_cPattern.isPhoto( ) )         { ++m_iCounterLikesPhoto; }
            else                                { ++m_iCounterLikesNotPhoto; }
            if( p_cPattern.isText( ) )          { ++m_iCounterLikesText; }
            else                                { ++m_iCounterLikesNotText; }
            if( p_cPattern.isLiked( ) )         { ++m_iCounterLikesLiked; }
            else                                { ++m_iCounterLikesNotLiked; }
            if( p_cPattern.isHot( ) )           { ++m_iCounterLikesHot; }
            else                                { ++m_iCounterLikesNotHot; }
            
            //ds and for each tag
            for( CTag cTag: p_cPattern.getTags( ) )
            {
                //ds get tag id
                final int iIDTag = cTag.getID( ); 
                
                //ds increment entry by 1
                m_mapCounterLikesTag.put( iIDTag, m_mapCounterLikesTag.get( iIDTag )+1 );
            }
        }
        else if( ELearnerLabel.DISLIKE == p_eLabel )
        {
            //ds increase counter
            ++m_iCounterDislikes;
        }
        
        //ds register pick
        m_vecIDsHistory.add( iIDCurrentPattern ); 
    }
    
    //ds resets counter for a processed element
    public final void _undoPick( final CPattern p_cPattern ) throws SQLException
    {
        //ds get the last element from picked
        final int iPreviousID = p_cPattern.getID( );
        
        //ds get the label for the last action
        final ELearnerLabel eLabel = m_mapLabels.get( iPreviousID );
        
        System.out.println( "[" + CLogger.getStamp( ) + "]<CLearnerBayes>(_undoPick) Relabeled ID: [" + iPreviousID + "]" );
        
        //ds decrease according counters
        if( ELearnerLabel.LIKE == eLabel )
        {
            //ds decrease counter
            --m_iCounterLikes; 
            
            //ds specifics TODO GIFS
            //if( cPattern.isAnimated( ) ){ --m_iCounterLikesAnimated; }
            //else                        { --m_iCounterLikesNotAnimated; }
            if( p_cPattern.isPhoto( ) )   { --m_iCounterLikesPhoto; }
            else                          { --m_iCounterLikesNotPhoto; }
            if( p_cPattern.isText( ) )    { --m_iCounterLikesText; }
            else                          { --m_iCounterLikesNotText; }
            if( p_cPattern.isLiked( ) )   { --m_iCounterLikesLiked; }
            else                          { --m_iCounterLikesNotLiked; }
            if( p_cPattern.isHot( ) )     { --m_iCounterLikesHot; }
            else                          { --m_iCounterLikesNotHot; }
            
            //ds and for each tag
            for( CTag cTag: p_cPattern.getTags( ) )
            {
                //ds get tag id
                final int iIDTag = cTag.getID( ); 
                
                //ds decrement entry by 1
                m_mapCounterLikesTag.put( iIDTag, m_mapCounterLikesTag.get( iIDTag )-1 );
            }
        }
        else if( ELearnerLabel.DISLIKE == eLabel )
        {
            --m_iCounterDislikes;
        }
        
        //ds remove entry from the map
        m_mapLabels.remove( iPreviousID );
    }
    
    //ds loads the initial dataset
    public final void fetchInitialDataPool( ) throws CZEPMySQLManagerException, SQLException, CZEPEoIException
    {       
        //ds initialize datapool
        m_vecDataPool = m_cMySQLManager.getDataset( m_iTagCutoffFrequency );
        
        //ds check consistency
        if( m_iNumberOfPatterns != m_vecDataPool.size( ) )
        {
            //ds escape
            throw new CZEPEoIException( "invalid fetching from database - dataset datapool mismatch" );             
        }        
        
        //ds shuffle datapool
        Collections.shuffle( m_vecDataPool );
        
        //ds initialize both selection pools (since we cannot classify from the beginning)
        m_vecSelectionPool     = new Vector< CPattern >( m_vecDataPool.subList( 0, m_iSizeSelection ) );
        m_vecSelectionPoolNext = new Vector< CPattern >( m_vecDataPool.subList( m_iSizeSelection, 2*m_iSizeSelection ) );
        
        //ds now remove the selection from the datapool
        m_vecDataPool.subList( 0, 2*m_iSizeSelection ).clear( );
    }
    
    //ds reset function for the learner
    public final void reset( ) throws MalformedURLException, CZEPMySQLManagerException, SQLException, CZEPEoIException
    {
        System.out.println( "[" + CLogger.getStamp( ) + "]<CLearnerBayes>(reset) Received reset request" );
        
        //ds add magic number 0 to history to mark reset
        m_vecIDsHistory.add( 0 );
        
        //ds reset datapool structures
        m_iIndexSelection = 0;
        
        //ds reset counters 
        m_iCounterLikes         = 0;
        m_iCounterDislikes      = 0;
        //m_iCounterLikesAnimated = 0; m_iCounterLikesNotAnimated = 0; TODO GIFS
        m_iCounterLikesPhoto    = 0; m_iCounterLikesNotPhoto    = 0;
        m_iCounterLikesText     = 0; m_iCounterLikesNotText     = 0;
        m_iCounterLikesLiked    = 0; m_iCounterLikesNotLiked    = 0;
        m_iCounterLikesHot      = 0; m_iCounterLikesNotHot      = 0;
        
        //ds clear labels
        m_mapLabels.clear( );
        
        //ds reset datapools completely
        fetchInitialDataPool( );

        //ds info
        System.out.println( "[" + CLogger.getStamp( ) + "]<CLearnerBayes>(reset) Learner has been reset" );
        _logMaster( "<CLearnerBayes>(reset) reset learning process" );
    }
    
    //ds statistic
    public final int getNumberOfLikes( ){ return m_iCounterLikes; }
    public final int getNumberOfDislikes( ){ return m_iCounterDislikes; }
    public final int getOperations( ){ return m_vecIDsHistory.size( ); }
    public final String getUsername( ){ return m_strUsername; }
    
    //ds setters
    public final void setUsername( final String p_strUsername )
    {
        //ds set username
        m_strUsername = p_strUsername; 
        
        //ds log setting
        System.out.println( "[" + CLogger.getStamp( ) + "]<CLearnerBayes>(setUsername) m_strUsername=" + p_strUsername );
        
        //ds and in mysql
        _logMaster( "<CLearnerBayes>(setUsername) new username: " + p_strUsername );
    }
    
    //ds main uses this method to check for classification needs
    public final boolean isReadyToClassify( )
    {
        //ds if we are ready and not classifying (should never be the case - except this method gets called from somewhere else than main)
        if( m_bIsReadyToClassify && !m_bIsClassifying )
        {
            //ds set the boolean that main is working on it
            m_bIsClassifying = true;
            
            //ds reset the ready boolean
            m_bIsReadyToClassify = false;
            
            //ds positive return
            return true;
        }
        else
        {
            //ds not ready
            return false;
        }
    }
    
    //ds evolve the datapool = classification
    public final void classify( ) throws CZEPEoIException
    {
        //ds classification was requested
        ++m_iCounterClassification;
        
        //ds random elements
        Vector< CPattern > vecRandomPool = new Vector< CPattern >( m_iNumberOfRandomSamples );
        
        //ds check if we have to add a random sample
        if( 0 == m_iCounterClassification%m_iIntervalRandomSample )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CLearnerBayes>(classify) Adding random samples: [" + m_iNumberOfRandomSamples + "]" );  
            
            //ds pick samples
            for( int i = 0; i < m_iNumberOfRandomSamples; ++i )
            {
                //ds get random index
                final int iIDRandom = new Random( ).nextInt( m_vecDataPool.size( ) );
                
                //ds pick a random pattern and add it
                vecRandomPool.add( m_vecDataPool.get( iIDRandom ).clone( ) );
                
                //ds remove it from the pool
                m_vecDataPool.remove( iIDRandom );
            }
        }
        
        //ds fancy Bayes operations starting
        System.out.println( "[" + CLogger.getStamp( ) + "]<CLearnerBayes>(classify) Classifying: [" + m_vecDataPool.size( ) + "] patterns" ); 
            
        //ds classify the patterns and obtain a sorted representation
        m_vecDataPool = _classifyPatterns( m_vecDataPool );
            
        //ds check if random
        if( 0 == m_iCounterClassification%m_iIntervalRandomSample )
        {
            //ds consistency check
            if( m_iNumberOfRandomSamples != vecRandomPool.size( ) )
            {
                //ds escape
                throw new CZEPEoIException( "invalid random fetching from datapool - random pool has invalid size" );                
            }
            
            //ds update next selection pool with random pool and classified pool
            m_vecSelectionPoolNext = new Vector< CPattern >( vecRandomPool );
            m_vecSelectionPoolNext.addAll( new Vector< CPattern >( m_vecDataPool.subList( 0, m_iSizeSelection-m_iNumberOfRandomSamples ) ) );   
            
            //ds remove selection from data pool
            m_vecDataPool.subList( 0, m_iSizeSelection-m_iNumberOfRandomSamples ).clear( );
            
            //ds info - we have to skip the random patterns since those have probability 0
            System.out.println( "[" + CLogger.getStamp( ) + "]<CLearnerBayes>(classify) maximum probability: [" + m_vecSelectionPoolNext.get( m_iNumberOfRandomSamples ).getLikeliness( ) + "]" );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CLearnerBayes>(classify) minimum probability: [" + m_vecSelectionPoolNext.lastElement( ).getLikeliness( ) + "]" );
        }
        else
        {
            //ds regular selection and clearance
            m_vecSelectionPoolNext = new Vector< CPattern >( m_vecDataPool.subList( 0, m_iSizeSelection ) );
            m_vecDataPool.subList( 0, m_iSizeSelection ).clear( );
            
            //ds info
            System.out.println( "[" + CLogger.getStamp( ) + "]<CLearnerBayes>(classify) maximum probability: [" + m_vecSelectionPoolNext.firstElement( ).getLikeliness( ) + "]" );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CLearnerBayes>(classify) minimum probability: [" + m_vecSelectionPoolNext.lastElement( ).getLikeliness( ) + "]" );
        }
        
        //ds classification done
        m_bIsClassifying = false;
    }
    
    //ds MySQL logger
    private final void _logMaster( final String p_strInfo )
    {
        //ds if set
        if( null != m_strUsername )
        {
            try
            {
                //ds log
                m_cMySQLManager.logMaster( m_strUsername, p_strInfo );
            }
            catch( SQLException e )
            {
                System.out.println( "[" + CLogger.getStamp( ) + "]<CLearnerBayes>(_logMaster) SQLException: " + e.getMessage( ) + " could not log to MySQL master" );
            }
        }
        else
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CLearnerBayes>(_logMaster) could not log to master because of empty username" );
        }
    }
    
    //ds classifies the given patterns
    private final Vector< CPattern > _classifyPatterns( final Vector< CPattern > p_vecPatterns )
    {
        //ds conditional probabilities
        final double dProbabilityLike              = 0.5 + ( double )( m_iCounterLikes-m_iCounterDislikes )/( 2*m_iNumberOfPatterns );
        //final double dProbabilityAnimatedIfLike    = ( double )m_iCounterLikesAnimated/m_iNumberOfPatterns;
        //final double dProbabilityNotAnimatedIfLike = ( double )m_iCounterLikesNotAnimated/m_iNumberOfPatterns;
        final double dProbabilityPhotoIfLike       = ( double )m_iCounterLikesPhoto/m_iNumberOfPatterns;
        final double dProbabilityNotPhotoIfLike    = ( double )m_iCounterLikesNotPhoto/m_iNumberOfPatterns;
        final double dProbabilityTextIfLike        = ( double )m_iCounterLikesText/m_iNumberOfPatterns;
        final double dProbabilityNotTextIfLike     = ( double )m_iCounterLikesNotText/m_iNumberOfPatterns;
        final double dProbabilityLikedIfLike       = ( double )m_iCounterLikesLiked/m_iNumberOfPatterns;
        final double dProbabilityNotLikedIfLike    = ( double )m_iCounterLikesNotLiked/m_iNumberOfPatterns;
        final double dProbabilityHotIfLike         = ( double )m_iCounterLikesHot/m_iNumberOfPatterns;
        final double dProbabilityNotHotIfLike      = ( double )m_iCounterLikesNotHot/m_iNumberOfPatterns; 
        
        //ds absolute probabilities
        //final double dProbabilityAnimated    = m_mapAbsoluteProbabilities.get( -1 );
        //final double dProbabilityNotAnimated = 1-m_mapAbsoluteProbabilities.get( -1 );
        final double dProbabilityPhoto       = m_mapAbsoluteProbabilities.get( -2 );
        final double dProbabilityNotPhoto    = 1-m_mapAbsoluteProbabilities.get( -2 );
        final double dProbabilityText        = m_mapAbsoluteProbabilities.get( -3 );
        final double dProbabilityNotText     = 1-m_mapAbsoluteProbabilities.get( -3 );
        final double dProbabilityLiked       = m_mapAbsoluteProbabilities.get( -4 );
        final double dProbabilityNotLiked    = 1-m_mapAbsoluteProbabilities.get( -4 );
        final double dProbabilityHot         = m_mapAbsoluteProbabilities.get( -5 );
        final double dProbabilityNotHot      = 1-m_mapAbsoluteProbabilities.get( -5 );
        
        //ds for each pattern
        for( CPattern cPattern: p_vecPatterns )
        {
            //ds probabilities to compute
            double dNominator   = 1.0;
            double dDenominator = 1.0;
            
            /*ds check which case we have TODO GIFs
            if( cPattern.isAnimated( ) )
            {
                dNominator   *= dProbabilityAnimatedIfLike;
                dDenominator *= dProbabilityAnimated;
            }
            else
            {
                dNominator   *= dProbabilityNotAnimatedIfLike;
                dDenominator *= dProbabilityNotAnimated;
            }*/
            
            if( cPattern.isPhoto( ) )
            {
                dNominator   *= dProbabilityPhotoIfLike;                
                dDenominator *= dProbabilityPhoto;
            }
            else
            {
                dNominator   *= dProbabilityNotPhotoIfLike;
                dDenominator *= dProbabilityNotPhoto;
            }
            
            if( cPattern.isText( ) )
            {
                dNominator   *= dProbabilityTextIfLike;
                dDenominator *= dProbabilityText;
            }
            else
            {
                dNominator   *= dProbabilityNotTextIfLike;
                dDenominator *= dProbabilityNotText;
            }
            
            if( cPattern.isLiked( ) )
            {
                dNominator   *= dProbabilityLikedIfLike;
                dDenominator *= dProbabilityLiked;
            }
            else
            {
                dNominator   *= dProbabilityNotLikedIfLike;
                dDenominator *= dProbabilityNotLiked;
            }
            
            if( cPattern.isHot( ) )
            {
                dNominator   *= dProbabilityHotIfLike;
                dDenominator *= dProbabilityHot;
            }
            else
            {
                dNominator   *= dProbabilityNotHotIfLike;
                dDenominator *= dProbabilityNotHot;
            }
          
            //ds now for the tags
            for( CTag cTag: cPattern.getTags( ) )
            {
                //ds tag id
                final int iIDTag = cTag.getID( );
                
                dNominator   *= ( double )m_mapCounterLikesTag.get( iIDTag )/m_mapNumbersOfTag.get( iIDTag );
                dDenominator *= m_mapAbsoluteProbabilities.get( iIDTag );
            }

            //ds modifiy the probability entry
            cPattern.setLikeliness( ( dNominator*dProbabilityLike )/dDenominator );     
            
            //ds and disable the randomness
            cPattern.setRandom( false );
        }
        
        //ds we now sort the vector
        Collections.sort( p_vecPatterns, new CPattern.CComparatorDecreasing( ) );
   
        //ds return the sorted patterns
        return p_vecPatterns;
    }
}
