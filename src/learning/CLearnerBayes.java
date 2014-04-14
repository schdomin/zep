package learning;

import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

//ds custom imports
import utility.CLogger;
import utility.CDataPoint;
import utility.CMySQLManager;
import exceptions.CZEPEoIException;
import exceptions.CZEPLearnerException;
import exceptions.CZEPMySQLManagerException;
import exceptions.CZEPnpIException;

public final class CLearnerBayes
{
    //ds enums
    public static enum ELearnerLabel{ LIKE, DISLIKE }

    //ds datapool elements
    private Vector< CDataPoint > m_vecDataPool          = new Vector< CDataPoint >( 0 );
    private List< CDataPoint > m_lstSelectionPoolActive = new ArrayList< CDataPoint >( );
    private List< CDataPoint > m_lstSelectionPoolNext   = new ArrayList< CDataPoint >( );
    private final int m_iSizeSelection                  = 10;
    private final int m_iSizeDatapool                   = 100;
    private int m_iSelectionCounter                     = 0;
    
    //ds sanity check
    private int m_iLastIDCycle = 0;
    
    //ds simple communication with main
    private boolean m_bIsReadyToClassify = false;
    private boolean m_bIsClassifying     = false;

    
    
    
    
    
    
    //ds likelihoods
    private double m_dLikelinessPhotograph = 0.0; private int m_iCounterLikesPhotograph = 0; private int m_iCounterDislikesPhotograph = 0;
    private double m_dLikelinessText       = 0.0;
    
    
    
    

    
    //ds vector with picked IDs and complete pick history
    //private Vector< Integer > m_vecIDsPicked  = new Vector< Integer >( 0 );
    private Vector< Integer > m_vecIDsHistory = new Vector< Integer >( 0 );
    
    
    //ds statistics: this vector keeps track of the LIKE/DISLIKE/.. sequence format: [id_datapoint][label]
    private Map< Integer, ELearnerLabel > m_mapLabels  = new HashMap< Integer, ELearnerLabel >( 0 );
    private int m_iNumberOfLikes    = 0;
    private int m_iNumberOfDislikes = 0;
    
    //ds mysql manager
    private final CMySQLManager m_cMySQLManager;
    
    //ds constructor
    public CLearnerBayes( final CMySQLManager p_cMySQLManager )
    {        
        //ds setup the manager
        m_cMySQLManager = p_cMySQLManager;
        
        //ds clean vectors
        //m_vecIDsPicked.clear( );
        m_vecIDsHistory.clear( );
        
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
    }
    
    //ds simple getter - does not change the request counter for an update cycle
    public final CDataPoint getFirstDataPoint( ) throws MalformedURLException, CZEPMySQLManagerException, SQLException
    {
        //ds register pick
        m_vecIDsHistory.add( m_lstSelectionPoolActive.get( 0 ).getID( ) );
        
        //ds retrieve the first datapoint
        return m_lstSelectionPoolActive.get( 0 );
    }
    
    //ds returns the next image
    public final CDataPoint getNextDataPoint( final ELearnerLabel p_eFlag, final CDataPoint p_cLastDataPoint ) throws CZEPEoIException, MalformedURLException, CZEPMySQLManagerException, SQLException, CZEPLearnerException
    {
        //ds get the id
        final int iCurrentImageID = p_cLastDataPoint.getID( );
        
        //ds check if we are the first image after classification
        if( 0 == m_iSelectionCounter )
        {
            //ds sanity check if we dont have the same image as in the last cycle
            if( m_iLastIDCycle != iCurrentImageID )
            {
                //ds update the id
                m_iLastIDCycle = iCurrentImageID;
            }
            else
            {
                //ds classification is not working - fatal
                throw new CZEPLearnerException( "sanity check failed same ID: " + iCurrentImageID + " no classification occured" );
            }
        }
        
        //ds save the flag for the last image
        m_mapLabels.put( iCurrentImageID, p_eFlag );
        
        //ds check if LIKE or DISLIKE
        if( ELearnerLabel.LIKE == p_eFlag )
        {
            //ds increase counter
            ++m_iNumberOfLikes;
            
            //ds if it was a photograph
            if( p_cLastDataPoint.isPhoto( ) ){ ++m_iCounterLikesPhotograph; }
        }
        else if( ELearnerLabel.DISLIKE == p_eFlag )
        {
            //ds increase counter
            ++m_iNumberOfDislikes;
            
            //ds if it was a photograph
            if( p_cLastDataPoint.isPhoto( ) ){ ++m_iCounterDislikesPhotograph; }
        }
        
        //ds register pick
        m_vecIDsHistory.add( iCurrentImageID );
        
        //ds increase pick counter
        ++m_iSelectionCounter;
        
        //ds check if we would access the image out of the selection buffer
        if( m_iSizeSelection <= m_iSelectionCounter )
        {
            //ds check if main is still busy from the last request
            if( m_bIsClassifying )
            {
                System.out.println( "[" + CLogger.getStamp( ) + "]<CLearnerBayes>(getNextDataPoint) Received classification request but main is busy" );                
            }
            
            //ds communicate with main over booleans we just have to set the ready boolean and go on
            m_bIsReadyToClassify = true;
            
            //ds reset counter
            m_iSelectionCounter = 0;
            
            //ds update selection pool
            m_lstSelectionPoolActive = m_lstSelectionPoolNext;
        }
        
        //ds and retrieve the next datapoint and return it
        return m_lstSelectionPoolActive.get( m_iSelectionCounter );
    }
    
    //ds returns the previous image - does not change the request counter for an update cycle
    public final CDataPoint getPreviousDataPoint( ) throws CZEPnpIException, MalformedURLException, CZEPMySQLManagerException, SQLException
    {
        //ds check if there is a previous image available (means at least 2 images picked by GUI)
        if( 0 < m_iSelectionCounter )
        {
            //ds move back to previous element
            --m_iSelectionCounter;
            
            //ds get the last element from picked
            final int iPreviousID = m_lstSelectionPoolActive.get( m_iSelectionCounter ).getID( );
            
            //ds get the label for the last action
            final ELearnerLabel eLabel = m_mapLabels.get( iPreviousID );
            
            //ds decrease according counter
            if(         ELearnerLabel.LIKE == eLabel ){ --m_iNumberOfLikes; }
            else if( ELearnerLabel.DISLIKE == eLabel ){ --m_iNumberOfDislikes; }   	    
            
            //ds remove entry from the map
            m_mapLabels.remove( iPreviousID );
            
            //ds register pick
            m_vecIDsHistory.add( iPreviousID );
            
            //ds return the according datapoint
            return m_lstSelectionPoolActive.get( m_iSelectionCounter );
        }
        else
        {
            //ds no previous picture available
            throw new CZEPnpIException( "No previous image available" );
        }
    }
    
    //ds loads the initial dataset
    public final void fetchInitialDataPool( ) throws MalformedURLException, CZEPMySQLManagerException, SQLException
    {
        //ds make sure the datapool is empty
        m_vecDataPool.clear( );
        
        //ds get datapoints from id 1 to selection size+pool size for the initialization
        m_vecDataPool = m_cMySQLManager.getDataPointsByIDRange( 1, m_iSizeDatapool+m_iSizeSelection );
        
        //ds initialize both selection pools (since we cannot classify from the beginning)
        m_lstSelectionPoolActive = new ArrayList< CDataPoint >( m_vecDataPool.subList( 0, m_iSizeSelection ) );
        m_lstSelectionPoolNext   = new ArrayList< CDataPoint >( m_vecDataPool.subList( m_iSizeSelection, 2*m_iSizeSelection ) );
        
        //ds now remove the selection from the datapool
        m_vecDataPool.subList( 0, 2*m_iSizeSelection ).clear( );
    }
    
    //ds reset function for the learner
    public final void reset( )
    {
        //ds add magic number 0 to history to mark reset
        m_vecIDsHistory.add( 0 );
        
        //ds reset datapool structures
        m_iSelectionCounter = 0;
        
        //ds sanity check
        m_iLastIDCycle      = 0;
        
        //ds and labels
        m_mapLabels.clear( );
        
        //ds reset internals
        m_iNumberOfLikes    = 0;
        m_iNumberOfDislikes = 0;
    }
    
    //ds statistic
    public final int getNumberOfVisits( ){ return 0; }
    public final int getNumberOfLikes( ){ return m_iNumberOfLikes; }
    public final int getOperations( ){ return m_vecIDsHistory.size( ); }
    
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
    public final void classify( ) throws MalformedURLException, CZEPMySQLManagerException, SQLException, InterruptedException
    {
        System.out.println( "[" + CLogger.getStamp( ) + "]<CLearnerBayes>(classify) Classifying" );     
        
        //ds test
        //Thread.sleep( 10000 );
        
        //ds compute new indices to fetch
        final int iIDStart = m_vecDataPool.lastElement( ).getID( )+1;
        final int iIDEnd   = iIDStart+m_iSizeSelection-1;
        
        //ds fetch new data
        m_vecDataPool.addAll( m_cMySQLManager.getDataPointsByIDRange( iIDStart, iIDEnd ) );
        
        //ds TODO fancy Bayes operations
        
        //ds update next selection pool
        m_lstSelectionPoolNext = new ArrayList< CDataPoint >( m_vecDataPool.subList( 0, m_iSizeSelection ) );
        
        //ds remove selection from data pool
        m_vecDataPool.subList( 0, m_iSizeSelection ).clear( );
        
        //ds classification done
        m_bIsClassifying = false;
    }
}
