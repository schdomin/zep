package learning;

import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.HashMap;
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

    
    
    
    //ds phases
    private boolean m_bIsCalibrationPhaseActive = false;
    
    
    
    
    
    
    //ds likelihoods
    private double m_dLikelinessPhotograph = 0.0; private int m_iCounterLikesPhotograph = 0; private int m_iCounterDislikesPhotograph = 0;
    private double m_dLikelinessText       = 0.0;
    
    
    
    

    
    //ds vector with picked IDs and complete pick history
    private Vector< Integer > m_vecIDsPicked  = new Vector< Integer >( );
    private Vector< Integer > m_vecIDsHistory = new Vector< Integer >( );
    
    
    //ds statistics: this vector keeps track of the LIKE/DISLIKE/.. sequence format: [id_datapoint][label]
    private Map< Integer, ELearnerLabel > m_mapLabels  = new HashMap< Integer, ELearnerLabel >( );
    private int m_iNumberOfLikes    = 0;
    private int m_iNumberOfDislikes = 0;
    private int m_iRequestCounter   = 1;
    
    //ds mysql manager
    private final CMySQLManager m_cMySQLManager;
    
    //ds constructor
    public CLearnerBayes( final CMySQLManager p_cMySQLManager )
    {        
        //ds setup the manager
        m_cMySQLManager = p_cMySQLManager;
        
        //ds activate calibration phase
        m_bIsCalibrationPhaseActive = true;
        
        //ds clean vectors
        m_vecIDsPicked.clear( );
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
        else
        {
            //ds do launch stuff
        }
    }
    
    //ds simple getter - does not change the request counter for an update cycle
    public final CDataPoint getFirstDataPoint( ) throws MalformedURLException, CZEPMySQLManagerException, SQLException
    {
        //ds register pick
        m_vecIDsHistory.add( 1 );
        
        //ds just retrieve the current data point
        return new CDataPoint( m_cMySQLManager.getDataPointByID( 1 ) );
    }
    
    //ds returns the next image
    public final CDataPoint getNextDataPoint( final ELearnerLabel p_eFlag, final CDataPoint p_cLastDataPoint ) throws CZEPEoIException, MalformedURLException, CZEPMySQLManagerException, SQLException, CZEPLearnerException
    {
        //ds sanity check
        if( m_mapLabels.size( ) != m_vecIDsPicked.size( ) ){ throw new CZEPLearnerException( "Internal datastructure size mismatch" ); }
        
        //ds increase request counter
        ++m_iRequestCounter;
        
        //ds get the id
        int iCurrentImageID = p_cLastDataPoint.getID( );
        
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
        
        //ds add to picked
        m_vecIDsPicked.add( iCurrentImageID );
        
        //ds check if we are in the calibration phase
        if( m_bIsCalibrationPhaseActive )
        {
            //ds just increment the current id
            ++iCurrentImageID;
            
            //ds register pick
            m_vecIDsHistory.add( iCurrentImageID );
            
            //ds and retrieve the next datapoint and return it
            return m_cMySQLManager.getDataPointByID( iCurrentImageID );
        }
        else
        {
            //ds register pick
            m_vecIDsHistory.add( iCurrentImageID );
            
            //ds fancy image selection
            return m_cMySQLManager.getDataPointByID( iCurrentImageID );
        }
    }
    
    //ds returns the previous image - does not change the request counter for an update cycle
    public final CDataPoint getPreviousDataPoint( ) throws CZEPnpIException, MalformedURLException, CZEPMySQLManagerException, SQLException
    {
    	//ds check if there is a previous image available (means at least 2 images picked by GUI)
    	if( 0 < m_vecIDsPicked.size( ) )
    	{
    	    //ds get the last element from picked
    	    final int iPreviousID = m_vecIDsPicked.lastElement( );
    	    
    	    //ds remove the element from the picked ones
    	    m_vecIDsPicked.remove( m_vecIDsPicked.size( )-1 );
    	    
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
    	    return m_cMySQLManager.getDataPointByID( iPreviousID );
    	}
    	else
    	{
    		//ds no previous picture available
    		throw new CZEPnpIException( "No previous image available" );
    	}
    }
    
    //ds reset function for the learner
    public void reset( )
    {
        //ds clear picked ids
        m_vecIDsPicked.clear( );
        
        //ds and labels
        m_mapLabels.clear( );
        
        //ds reset internals
        m_iRequestCounter   = 1; 
        m_iNumberOfLikes    = 0;
        m_iNumberOfDislikes = 0;
    }
    
    //ds statistic
    public final int getNumberOfVisits( ){ return m_vecIDsPicked.size( ); }
    public final int getNumberOfLikes( ){ return m_iNumberOfLikes; }
    public final int getNumberOfDislikes( ){ return m_iNumberOfDislikes; }
    public final int getRequests( ){ return m_iRequestCounter; }
    
    //ds retrieve a new image id
    private int _getNewImageID( )
    {
    	//ds determine the next id to pick
    	//final int iIndex = 1;
    	
    	//ds access the element in the vector
    	//final int iImageID = m_vecImageIDsAvailable.elementAt( iIndex );
    	
    	//ds we remove the ID from the availability vector and add it to the picked ones
    	//m_vecImageIDsAvailable.remove( iIndex );
    	//m_vecImageIDsPicked.add( iImageID );
    	
    	//ds return the ID
    	return 0;
    }
}
