package learning;

import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import utility.CIndexer;
//ds custom imports
import utility.CLogger;
import utility.CMySQLManager;
import utility.CPattern;
import utility.CTag;
import exceptions.CZEPEoIException;
import exceptions.CZEPLearnerException;
import exceptions.CZEPMySQLManagerException;
import exceptions.CZEPnpIException;

public final class CLearnerBayes
{
    //ds enums
    public static enum ELearnerLabel{ LIKE, DISLIKE }

    //ds data pool control elements
    private final int m_iSizeSelection   = 10;
    private final int m_iSizeDatapool    = 100;
    private int m_iIndexSelection        = 0;
    private int m_iIndexPrevious         = 0;
    private int m_iCounterClassification = 0;
    
    //ds pools
    private Vector< CPattern > m_vecDataPool          = null;
    private Vector< CPattern > m_vecSelectionPool     = null;
    private Vector< CPattern > m_vecSelectionPoolNext = null;
    
    //ds random sample frequency - every n times (for 1 there is no classification done forever)
    private final int m_iIntervalRandomSample = 5;
    private boolean m_bWasPreviousRequested   = false;
    
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
    
    //ds indexer
    private CIndexer m_cIndexer = null;
    
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
    public void launch( ) throws SQLException, CZEPMySQLManagerException, CZEPEoIException
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
        final Vector< Integer > vecTagIDs = m_cMySQLManager.getTagIDs( m_iTagCutoffFrequency );
        
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
        
        //ds initialize indexer to pick patterns
        m_cIndexer = new CIndexer( m_iNumberOfPatterns );
    }
    
    //ds simple getter - does not change the request counter for an update cycle
    public final CPattern getFirstDataPoint( ) throws MalformedURLException, CZEPMySQLManagerException, SQLException
    {
        //ds register pick
        m_vecIDsHistory.add( m_vecSelectionPool.get( 0 ).getID( ) );
        
        //ds retrieve the first datapoint
        return m_vecSelectionPool.get( 0 );
    }
    
    //ds returns the next image
    public final CPattern getNextPattern( final ELearnerLabel p_eFlag, final CPattern p_cLastPattern ) throws CZEPEoIException, MalformedURLException, CZEPMySQLManagerException, SQLException, CZEPLearnerException
    {
        //ds check if we land here after a previous call - which means we have to undo the settings for that pattern
        if( m_bWasPreviousRequested )
        {
            //ds undo the pick for the previous pattern before registering the new settings
            _undoPick( p_cLastPattern );
            
            //ds disable previous phase
            m_bWasPreviousRequested = false;
            
            //ds decrease pick counter by one, we want to see the image again at the previous start
            --m_iIndexSelection;
        }
        
        //ds evaluate the pick
        _evaluatePick( p_cLastPattern, p_eFlag );
        
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
            m_vecSelectionPool.addAll( m_vecSelectionPoolNext );
            
            //ds clear next
            m_vecSelectionPoolNext.clear( );
        }
        
        //ds and retrieve the next datapoint and return it
        return m_vecSelectionPool.get( m_iIndexSelection );
    }
    
    //ds returns the previous image - does not change the request counter for an update cycle
    public final CPattern getPreviousPattern( ) throws CZEPnpIException, MalformedURLException, CZEPMySQLManagerException, SQLException
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
            if( p_cPattern.isPhoto( ) )   { ++m_iCounterLikesPhoto; }
            else                              { ++m_iCounterLikesNotPhoto; }
            if( p_cPattern.isText( ) )    { ++m_iCounterLikesText; }
            else                              { ++m_iCounterLikesNotText; }
            if( p_cPattern.isLiked( ) )   { ++m_iCounterLikesLiked; }
            else                              { ++m_iCounterLikesNotLiked; }
            if( p_cPattern.isHot( ) )     { ++m_iCounterLikesHot; }
            else                              { ++m_iCounterLikesNotHot; }
            
            //ds and for each tag
            for( CTag cTag: p_cPattern.getTags( ) )
            {
                //ds get tag id
                final int iIDTag = cTag.getID( ); 
                
                //ds increment entry by 1
                m_mapCounterLikesTag.put( iIDTag, m_mapCounterLikesTag.get( iIDTag )+1 );
            }
            
            //ds log to learner
            m_cMySQLManager.logPattern( m_strUsername, p_cPattern, true );
        }
        else if( ELearnerLabel.DISLIKE == p_eLabel )
        {
            //ds increase counter
            ++m_iCounterDislikes;
            
            //ds log to learner
            m_cMySQLManager.logPattern( m_strUsername, p_cPattern, false );
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
        
        //ds decrease according counters
        if( ELearnerLabel.LIKE == eLabel )
        {
            //ds decrease counter
            --m_iCounterLikes; 
            
            //ds specifics TODO GIFS
            //if( cPattern.isAnimated( ) ){ --m_iCounterLikesAnimated; }
            //else                        { --m_iCounterLikesNotAnimated; }
            if( p_cPattern.isPhoto( ) )   { --m_iCounterLikesPhoto; }
            else                        { --m_iCounterLikesNotPhoto; }
            if( p_cPattern.isText( ) )    { --m_iCounterLikesText; }
            else                        { --m_iCounterLikesNotText; }
            if( p_cPattern.isLiked( ) )   { --m_iCounterLikesLiked; }
            else                        { --m_iCounterLikesNotLiked; }
            if( p_cPattern.isHot( ) )     { --m_iCounterLikesHot; }
            else                        { --m_iCounterLikesNotHot; }
            
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
        
        //ds remove entry from mysql
        m_cMySQLManager.dropPattern( m_strUsername, p_cPattern );
        
        //ds remove entry from the map
        m_mapLabels.remove( iPreviousID );
    }
    
    //ds loads the initial dataset
    public final void fetchInitialDataPool( ) throws MalformedURLException, CZEPMySQLManagerException, SQLException, CZEPEoIException
    {       
        //ds get datapoints from id 1 to selection size+pool size for the initialization
        m_vecDataPool = _getPatterns( m_iSizeDatapool+m_iSizeSelection );
        
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
        
        //ds reset indexer
        m_cIndexer.reset( );
        
        //ds clear labels
        m_mapLabels.clear( );
        
        //ds reset datapools
        fetchInitialDataPool( );

        //ds info
        System.out.println( "[" + CLogger.getStamp( ) + "]<CLearnerBayes>(reset) Learner has been reset" );
        _logMaster( "<CLearnerBayes>(reset) reset learning process" );
    }
    
    //ds statistic
    public final int getNumberOfVisits( ){ return 0; }
    public final int getNumberOfLikes( ){ return m_iCounterLikes; }
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
    public final void classify( ) throws MalformedURLException, CZEPMySQLManagerException, SQLException, InterruptedException, CZEPEoIException
    {
        //ds classification was requested
        ++m_iCounterClassification;
        
        //ds check if we just have to add a random sample to the selection pool
        if( 0 == m_iCounterClassification%m_iIntervalRandomSample )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CLearnerBayes>(classify) Fetching Random Sample .." );  
            
            //ds update next selection pool with random samples
            m_vecSelectionPoolNext = _getPatterns( m_iSizeSelection );
        }
        else
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CLearnerBayes>(classify) Fetching .." ); 
            
            //ds fetch new data
            m_vecDataPool.addAll( _getPatterns( m_iSizeSelection ) );
            
            //ds fancy Bayes operations starting
            System.out.println( "[" + CLogger.getStamp( ) + "]<CLearnerBayes>(classify) Classifying .." ); 
            
            //ds classify the patterns and obtain a sorted version
            m_vecDataPool = _classifyPatterns( m_vecDataPool );
            
            //ds update next selection pool with the images classified best (starting at the beginning of the datapool)
            m_vecSelectionPoolNext = new Vector< CPattern >( m_vecDataPool.subList( 0, m_iSizeSelection ) );   
            
            //ds remove selection from data pool
            m_vecDataPool.subList( 0, m_iSizeSelection ).clear( );
        }
        
        //ds classification done
        m_bIsClassifying = false;
        
        System.out.println( "[" + CLogger.getStamp( ) + "]<CLearnerBayes>(classify) Done" ); 
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
    
    //ds gets a certain number of patterns
    private final Vector< CPattern > _getPatterns( final int p_iNumberOfPatterns ) throws CZEPEoIException, CZEPMySQLManagerException, SQLException
    {
        System.out.println( "[" + CLogger.getStamp( ) + "]<CLearnerBayes>(_getPatterns) received fetch request for patterns: " + p_iNumberOfPatterns );
        
        //ds pattern vector
        Vector< CPattern > vecPatterns = new Vector< CPattern >( p_iNumberOfPatterns );
        
        //ds we have to pick the desired number of patterns
        for( int i = 0; i < p_iNumberOfPatterns; ++i )
        {
            //ds fetch the element from MySql
            vecPatterns.add( m_cMySQLManager.getPatternByID( m_cIndexer.drawID( ), m_iTagCutoffFrequency ) );
        }
        
        //ds return the vector
        return vecPatterns;
    }
    
    //ds classifies the given patterns
    private final Vector< CPattern > _classifyPatterns( final Vector< CPattern > p_vecPatterns ) throws SQLException
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
