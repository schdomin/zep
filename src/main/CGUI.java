package main;

import java.net.MalformedURLException;
import java.sql.SQLException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.Border;

import learning.CLearnerBayes;
//ds custom imports
//import learning.CLearnerRandom;
import exceptions.CZEPEoIException;
import exceptions.CZEPGUIException;
import exceptions.CZEPLearnerException;
import exceptions.CZEPMySQLManagerException;
import exceptions.CZEPnpIException;
import utility.CDataPoint;
import utility.CImageHandler;
import utility.CLogger;
import utility.CMySQLManager;
import utility.CPattern;

public final class CGUI implements ActionListener, KeyListener
{
    //ds GUI frame handle for manipulation
    JFrame m_cFrame = new JFrame( "ZEP: Zero-Effort Procrastination" );
    
    //ds image display
    private final JLabel m_cLabelImage = new JLabel( "", JLabel.CENTER );
    
    //ds panels
    private final JPanel m_cPanelNorth       = new JPanel( );
    private final JPanel m_cPanelEast        = new JPanel( );
    private final JPanel m_cPanelSouth       = new JPanel( );
    //private final JPanel m_cPanelWest        = new JPanel( );
    private final JScrollPane m_cPanelCImage = new JScrollPane( m_cLabelImage );
    
    //ds components
    private final JButton m_cButtonReset    = new JButton( "Reset (R)" );;
    private final JButton m_cButtonLike     = new JButton( "Like" );
    private final JButton m_cButtonDislike  = new JButton( "Dislike" );
    private final JButton m_cButtonPrevious = new JButton( "Previous" );
    
    //ds textfields
    private final JTextField m_cTextFieldTitle       = new JTextField( 60 );
    private final JTextField m_cTextFieldURL         = new JTextField( 60 );
    private final JTextField m_cTextFieldTags        = new JTextField( 60 );
    private final JTextField m_cTextFieldImageID     = new JTextField( 3 );
    private final JTextField m_cTextFieldLikes       = new JTextField( 3 );
    private final JTextField m_cTextFieldDislikes    = new JTextField( 3 );
    private final JTextField m_cTextFieldVisits      = new JTextField( 3 );
    private final JTextField m_cTextFieldConfidence  = new JTextField( 3 );
    private final JTextField m_cTextFieldType        = new JTextField( 3 );
    private final JTextField m_cTextFieldDatasetSize = new JTextField( 3 );
    private final JTextField m_cTextFieldTextPercent = new JTextField( 3 );
    private final JTextField m_cTextFieldIsPhoto     = new JTextField( 3 );
    private final JTextField m_cTextFieldComments    = new JTextField( 3 );
    private final JTextField m_cTextFieldTagsCount   = new JTextField( 3 );
    private final JTextField m_cTextFieldTotalLikes  = new JTextField( 3 );
    
    //ds fonts
    Font m_cFontTitle = new Font( m_cTextFieldTitle.getFont( ).getName( ), Font.BOLD, 18 );
    
    //ds interpreter - all calls go over this object
    //private final CLearnerRandom m_cLearner;
    private final CLearnerBayes m_cLearner;
    
    //ds MySQL calls
    private final CMySQLManager m_cMySQLManager;
    
    //ds currently active datapoint
    private CPattern m_cCurrentPattern = null;
    
    //ds required for smooth first image display
    final int m_iWindowWidth;
    final int m_iWindowHeight;
    
    //ds constructor
    public CGUI( final CLearnerBayes p_cLearner, final CMySQLManager p_cMySQLManager, final int p_iWindowWidth, final int p_iWindowHeight )
    {        
        //ds set the learner
        m_cLearner = p_cLearner;
        
        //ds and MySQL manager
        m_cMySQLManager = p_cMySQLManager;
        
        //ds set constants
        m_iWindowWidth  = p_iWindowWidth;
        m_iWindowHeight = p_iWindowHeight;
        
        //ds configure the frame
        m_cFrame.setSize( p_iWindowWidth, p_iWindowHeight );
        //m_cFrame.setResizable( false );
        m_cFrame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        
        //ds divide frame in two parts: information and image
        m_cFrame.setLayout( new BorderLayout( ) );
        
        //ds creating a border to highlight the JPanel areas
        Border cOutline = BorderFactory.createLineBorder( Color.black );
        
        //ds set borders
        //m_cPanelNorth.setBorder( cOutline );
        //m_cPanelEast.setBorder( cOutline );
        //m_cPanelSouth.setBorder( cOutline );
        //m_cPanelWest.setBorder( cOutline );
        m_cPanelCImage.setBorder( cOutline );
        
        //ds setup components (buttons, ..)
        _setComponents( );
        
        //ds add the panels to the frame
        m_cFrame.add( m_cPanelNorth , BorderLayout.NORTH );
        //m_cFrame.add( m_cPanelEast  , BorderLayout.EAST );
        m_cFrame.add( m_cPanelSouth , BorderLayout.SOUTH );
        //m_cFrame.add( m_cPanelWest  , BorderLayout.WEST );
        m_cFrame.add( m_cPanelCImage, BorderLayout.CENTER );
        
        System.out.println( "[" + CLogger.getStamp( ) + "]<CGUI>(CGUI) Instance allocated" );
    }
    
    //ds enable display
    public void launch( ) throws CZEPGUIException, SQLException, CZEPMySQLManagerException
    {
        //ds allocate a dialog object to display independently
        final JDialog cDialogLoading = new JDialog( m_cFrame, "ZEP: Zero-Effort Procrastination", false );
        
        //ds set the option panel without any options
        cDialogLoading.setContentPane( new JOptionPane( "Loading image data please wait", JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null ) );
        
        //ds display the dialog
        cDialogLoading.pack( );
        cDialogLoading.setVisible( true );
        cDialogLoading.setLocationRelativeTo( null );
        
        try
        {
            //ds fetch initial datapool
            m_cLearner.fetchInitialDataPool( );
    	
            //ds try to get the image for first display
            _displayInitialImage( m_cLearner.getFirstDataPoint( ) );
        }
        catch( Exception e )
        {
            //ds info
            System.out.println( "[" + CLogger.getStamp( ) + "]<CGUI>(launch) Exception: " + e.getMessage( ) + " - could not fetch database" );
            
            //ds dispose loading screen
            cDialogLoading.removeAll( );
            cDialogLoading.dispose( );   
            
            //ds rethrow
            throw new CZEPGUIException( "GUI aborted" );
        }
        
        //ds dispose loading screen
        cDialogLoading.removeAll( );
        cDialogLoading.dispose( );
        
        //ds register key listener
        m_cFrame.addKeyListener( this );
        
        //ds display frame for interaction
        m_cFrame.setVisible( true );  
        m_cFrame.setLocationRelativeTo( null );

        //ds request focus for key strokes
        m_cFrame.requestFocus( );
        
        //ds initialize with empty string
        String strUsername = "";
        
        //ds as long as it is not set
        while( strUsername.isEmpty( ) )
        {
            //ds show dialog to enter name
            strUsername = JOptionPane.showInputDialog( m_cFrame, "Please enter your desired username: ", "ZEP: Zero-Effort Procrastination", JOptionPane.PLAIN_MESSAGE );
            
            //ds check if null (cancelled by user)
            if( null == strUsername )
            {
                //ds escape
                throw new CZEPGUIException( "cancelled username setting dialog" );
            }
            
            //ds check if already taken
            if( !strUsername.isEmpty( ) && !m_cMySQLManager.isUserAvailable( strUsername ) )
            {
                //ds inform
                JOptionPane.showMessageDialog( m_cFrame, "Username already taken - please try again" );
                
                //ds keep looping
                strUsername = "";
            }
        }
        
        //ds username is fine to set
        m_cLearner.setUsername( strUsername );

        //ds set user active
        m_cMySQLManager.setActiveUser( strUsername );
        
        //ds and log
        System.out.println( "[" + CLogger.getStamp( ) + "]<CGUI>(launch) Login of: [" + strUsername + "] successful" );
        
        //ds log successful launch
        _logMaster( "<CGUI>(launch) launched GUI application" );
        
        //ds request focus for key strokes
        m_cFrame.requestFocus( );
    }
    
    //ds check if active
    public boolean isActive( )
    {
        return m_cFrame.isShowing( );
    }
    
    //ds close GUI
    public void close( )
    {
        //ds get username
        final String strUsername = m_cLearner.getUsername( );
        
        //ds if username is set
        if( null != strUsername )
        {
            //ds log
            _logMaster( "<CGUI>(close) closed GUI application" );
            
            try
            {
                //ds remove user from active list
                m_cMySQLManager.removeActiveUser( strUsername );
                
                //ds log
                System.out.println( "[" + CLogger.getStamp( ) + "]<CGUI>(close) Logout of: [" + strUsername + "] successful" );
            }
            catch( Exception e )
            {
                //ds could not remove
                System.out.println( "[" + CLogger.getStamp( ) + "]<CGUI>(close) Could not remove user: [" + strUsername + "] from active users list - please check database" );
            }
        }

        //ds only if active
        if( isActive( ) )
        {
            //ds dispose frame completely
            m_cFrame.removeAll( );
            m_cFrame.dispose( );
        }
        
        //ds info
        System.out.println( "[" + CLogger.getStamp( ) + "]<CGUI>(close) Closing of GUI complete" );
    }
  
    //ds event invoked function
    public void actionPerformed( ActionEvent p_cEvent )
    {
        //ds get the event source
        final Object cSource = p_cEvent.getSource( );
        
        try
        {
            //ds determine event (big if/else tree)
            if(      cSource == m_cButtonLike ){ _displayImage( m_cLearner.getNextPattern( CLearnerBayes.ELearnerLabel.LIKE, m_cCurrentPattern ) ); }
            else if( cSource == m_cButtonDislike ){ _displayImage( m_cLearner.getNextPattern( CLearnerBayes.ELearnerLabel.DISLIKE, m_cCurrentPattern ) ); }
            else if( cSource == m_cButtonPrevious ){ _displayImage( m_cLearner.getPreviousPattern( ) ); }
            else if( cSource == m_cButtonReset )
            {
                //ds reset the learning process
                m_cLearner.reset( );
                
                //ds get a new image
                _displayImage( m_cLearner.getFirstDataPoint( ) );
            }
        }
        catch( SQLException e )
        {
            _logMaster( "<CGUI>(actionPerformed) SQLException: " + e.getMessage( ) );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CGUI>(actionPerformed) SQLException: " + e.getMessage( ) );
            
            //ds no previous image available
            JOptionPane.showMessageDialog( m_cFrame, "Could not load image from MySQL database" );            
        }
        catch( CZEPnpIException e )
        {
            _logMaster( "<CGUI>(actionPerformed) CZEPnpIException: " + e.getMessage( ) );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CGUI>(actionPerformed) CZEPnpIException: " + e.getMessage( ) );
            
            //ds no previous image available
            JOptionPane.showMessageDialog( m_cFrame, "No previous image available - please use Like/Dislike to classify the first image" );         
        }
        catch( CZEPMySQLManagerException e )
        {
            _logMaster( "<CGUI>(actionPerformed) CZEPMySQLManagerException: " + e.getMessage( ) );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CGUI>(actionPerformed) CZEPMySQLManagerException: " + e.getMessage( ) );
            
            //ds no previous image available
            JOptionPane.showMessageDialog( m_cFrame, "Could not load image from MySQL database" );         
        }
        catch( MalformedURLException e )
        {
            _logMaster( "<CGUI>(actionPerformed) MalformedURLException: " + e.getMessage( ) );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CGUI>(actionPerformed) MalformedURLException: " + e.getMessage( ) );
            
            //ds no previous image available
            JOptionPane.showMessageDialog( m_cFrame, "Could not load image from MySQL database" );                
        }
        catch( CZEPEoIException e )
        {
            _logMaster( "<CGUI>(actionPerformed) CZEPEoIException: " + e.getMessage( ) );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CGUI>(actionPerformed) CZEPEoIException: " + e.getMessage( ) );
            
            //ds no previous image available
            JOptionPane.showMessageDialog( m_cFrame, "Could not load image from MySQL database" );                
        }
        catch( CZEPLearnerException e )
        {
            _logMaster( "<CGUI>(actionPerformed) CZEPLearnerException: " + e.getMessage( ) );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CGUI>(actionPerformed) CZEPLearnerException: " + e.getMessage( ) );
            
            //ds no previous image available
            JOptionPane.showMessageDialog( m_cFrame, "Internal learning error - please reset" );                
        }
    }
    
    //ds KeyListener - just map the keys to the buttons
    public void keyPressed( KeyEvent p_cKey )
    {
        //ds get the keycode
        final int iKeyCode = p_cKey.getKeyCode( );
        
        //ds if/else tree
        if(      iKeyCode == KeyEvent.VK_RIGHT ){ m_cButtonLike.doClick( ); }
        else if( iKeyCode == KeyEvent.VK_DOWN ){ m_cButtonDislike.doClick( ); }
        else if( iKeyCode == KeyEvent.VK_LEFT ){ m_cButtonPrevious.doClick( ); }
        else if( iKeyCode == KeyEvent.VK_R ){ m_cButtonReset.doClick( ); }
        else if( iKeyCode == KeyEvent.VK_ESCAPE )
        {
            //ds notify
            System.out.println( "[" + CLogger.getStamp( ) + "]<CGUI>(keyPressed) Caught escape signal - shutting down GUI" );
            
            //ds escape gui
            m_cFrame.removeAll( );
            m_cFrame.dispose( );
        }
    }
    
    //ds update GUI with new image
    private void _displayImage( final CPattern p_cPattern ) throws CZEPMySQLManagerException
    {
        //ds update active datapoint
        m_cCurrentPattern = p_cPattern;
        
    	//ds if we got a gif we take the image as is
    	if( p_cPattern.isAnimated( ) )
    	{
    		//ds set the icon
    		m_cLabelImage.setIcon( m_cMySQLManager.getImageIcon( p_cPattern ) );
    	}
    	else
    	{
    		//ds set the image to the GUI field (resized)
    		m_cLabelImage.setIcon( new ImageIcon( CImageHandler.getResizedImage( m_cMySQLManager.getBufferedImage( p_cPattern ), m_cPanelCImage.getWidth( )-5, m_cPanelCImage.getHeight( )-5 ) ) );
    	}
        
        //ds update image info
        m_cTextFieldTitle.setText( p_cPattern.getTitle( ) );
    }
    
    //ds update GUI with new image
    private void _displayInitialImage( final CPattern p_cPattern ) throws CZEPMySQLManagerException
    {
        //ds update active datapoint
        m_cCurrentPattern = p_cPattern;
        
        //ds if we got a gif we take the image as is
        if( p_cPattern.isAnimated( ) )
        {
            //ds set the icon
            m_cLabelImage.setIcon( m_cMySQLManager.getImageIcon( p_cPattern ) );
        }
        else
        {
            //ds set the image to the GUI field (resized)
            m_cLabelImage.setIcon( new ImageIcon( CImageHandler.getResizedImage( m_cMySQLManager.getBufferedImage( p_cPattern ), m_iWindowWidth-5, m_iWindowHeight-125 ) ) );
        }
        
        //ds update image info
        m_cTextFieldTitle.setText( p_cPattern.getTitle( ) );
        //m_cTextFieldURL.setText( p_cPattern.getURL( ).toString( ) );
        m_cTextFieldTags.setText( p_cPattern.getTags( ).toString( ) );
        
        //ds datapoint properties
        m_cTextFieldImageID.setText( Integer.toString( p_cPattern.getID( ) ) );
        //m_cTextFieldType.setText( p_cPattern.getType( ) );
        //m_cTextFieldLikes.setText( Integer.toString( p_cPattern.getLikes( ) ) );
        //m_cTextFieldDislikes.setText( Integer.toString( p_cPattern.getDislikes( ) ) );
        
        //ds check if gif
        if( p_cPattern.isAnimated( ) )
        {
            //ds no detection done
            m_cTextFieldTextPercent.setText( "" );
            m_cTextFieldIsPhoto.setText( "" );
        }
        else
        {
            //ds get the values
            m_cTextFieldTextPercent.setText( Boolean.toString( p_cPattern.isAnimated( ) ) );
            m_cTextFieldIsPhoto.setText( Boolean.toString( p_cPattern.isPhoto( ) ) );            
        }
        
        
        //m_cTextFieldComments.setText( Integer.toString( p_cPattern.getCountComments( ) ) );
        //m_cTextFieldTagsCount.setText( Integer.toString( p_cPattern.getCountTags( ) ) );
        
        //ds learner
        m_cTextFieldVisits.setText( Integer.toString( m_cLearner.getNumberOfVisits( ) ) );
        m_cTextFieldDatasetSize.setText( Integer.toString( m_cLearner.getOperations( ) ) );
        m_cTextFieldTotalLikes.setText( Integer.toString( m_cLearner.getNumberOfLikes( ) ) );
    }
    
    //ds component setup
    private void _setComponents( )
    {
        //ds scroll speed of panel
        m_cPanelCImage.getVerticalScrollBar( ).setUnitIncrement( 25 );
        
        //ds register the buttons
        m_cButtonReset.addActionListener( this );
        m_cButtonReset.addKeyListener( this );
        m_cButtonDislike.addActionListener( this );
        m_cButtonDislike.addKeyListener( this );
        m_cButtonLike.addActionListener( this );
        m_cButtonLike.addKeyListener( this );
        m_cButtonPrevious.addActionListener( this );
        m_cButtonPrevious.addKeyListener( this );
        
        //ds customize mechanical buttons
        m_cButtonReset.setPreferredSize( new Dimension( 120, 25 ) );
        m_cButtonPrevious.setPreferredSize( new Dimension( 110, 30 ) );
        
        //ds customize like buttons
        m_cButtonDislike.setBackground( Color.red );
        m_cButtonLike.setBackground( Color.green );
        m_cButtonDislike.setPreferredSize( new Dimension( 110, 30 ) );
        m_cButtonLike.setPreferredSize( new Dimension( 110, 30 ) );
        
        //ds button icons
        final ImageIcon cIconLike     = new ImageIcon( "icons/like_resized.png" );
        final ImageIcon cIconDislike  = new ImageIcon( "icons/dislike_resized.png" );
        final ImageIcon cIconPrevious = new ImageIcon( "icons/back_resized.png" );
        
        //ds customize
        m_cButtonLike.setIcon( cIconLike );
        m_cButtonDislike.setIcon( cIconDislike );
        m_cButtonPrevious.setIcon( cIconPrevious );
        m_cButtonLike.setMargin( new Insets( 0, 0, 0, 0 ) );
        m_cButtonDislike.setMargin( new Insets( 0, 0, 0, 0 ) );
        m_cButtonPrevious.setMargin( new Insets( 0, 0, 0, 0 ) );
        m_cButtonLike.setForeground( Color.darkGray );
        m_cButtonDislike.setForeground( Color.white );
        m_cButtonPrevious.setForeground( Color.darkGray );
        
        //ds textfields
        m_cTextFieldTitle.setEditable( false );
        m_cTextFieldTitle.setFont( m_cFontTitle );
        m_cTextFieldURL.setEditable( false );
        m_cTextFieldTags.setEditable( false );
        m_cTextFieldImageID.setEditable( false );
        m_cTextFieldLikes.setEditable( false );
        m_cTextFieldDislikes.setEditable( false );
        m_cTextFieldVisits.setEditable( false );
        m_cTextFieldConfidence.setEditable( false );
        m_cTextFieldType.setEditable( false );
        m_cTextFieldDatasetSize.setEditable( false );
        m_cTextFieldTextPercent.setEditable( false );
        m_cTextFieldIsPhoto.setEditable( false );
        m_cTextFieldComments.setEditable( false );
        m_cTextFieldTagsCount.setEditable( false );
        m_cTextFieldTotalLikes.setEditable( false );
        
        //ds add the sidebar panels
        final JPanel cPanelImageID     = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelLikes       = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelDislikes    = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelVisits      = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelConfidence  = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelType        = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelDatasetSize = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelTextPercent = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelIsPhoto     = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelComments    = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelTagsCount   = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelTotalLikes  = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        
        //ds structural side bar
        final JPanel cPanelProperties = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelLearning   = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelWhitespace = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        
        //ds add labels and textfield to each panel
        cPanelImageID.add( new JLabel( "MySQL ID: " ) );        cPanelImageID.add( m_cTextFieldImageID );
        cPanelLikes.add( new JLabel( "Likes: " ) );             cPanelLikes.add( m_cTextFieldLikes );
        cPanelDislikes.add( new JLabel( "Dislikes: " ) );       cPanelDislikes.add( m_cTextFieldDislikes );
        cPanelVisits.add( new JLabel( "Visited Images: " ) );   cPanelVisits.add( m_cTextFieldVisits );
        cPanelConfidence.add( new JLabel( "Confidence: " ) );   cPanelConfidence.add( m_cTextFieldConfidence );
        cPanelType.add( new JLabel( "Type: " ) );               cPanelType.add( m_cTextFieldType );
        cPanelDatasetSize.add( new JLabel( "Operations: " ) );  cPanelDatasetSize.add( m_cTextFieldDatasetSize );
        cPanelTextPercent.add( new JLabel( "Text Amount: " ) ); cPanelTextPercent.add( m_cTextFieldTextPercent );
        cPanelIsPhoto.add( new JLabel( "Photograph: " ) );      cPanelIsPhoto.add( m_cTextFieldIsPhoto );
        cPanelComments.add( new JLabel( "Comments: " ) );       cPanelComments.add( m_cTextFieldComments );
        cPanelTagsCount.add( new JLabel( "Tags Count: " ) );    cPanelTagsCount.add( m_cTextFieldTagsCount );
        cPanelTotalLikes.add( new JLabel( "Total Likes: " ) );  cPanelTotalLikes.add( m_cTextFieldTotalLikes );
        
        cPanelProperties.add( new JLabel( "Properties                    " ) );
        cPanelLearning.add(   new JLabel( "Learning                       " ) );
        cPanelWhitespace.add( new JLabel( "" ) );
        
        //ds set maximum size to align vertically
        cPanelImageID.setMaximumSize( cPanelImageID.getPreferredSize( ) );
        cPanelLikes.setMaximumSize( cPanelLikes.getPreferredSize( ) );
        cPanelDislikes.setMaximumSize( cPanelDislikes.getPreferredSize( ) );
        cPanelVisits.setMaximumSize( cPanelVisits.getPreferredSize( ) );
        cPanelConfidence.setMaximumSize( cPanelConfidence.getPreferredSize( ) );
        cPanelType.setMaximumSize( cPanelType.getPreferredSize( ) );
        cPanelDatasetSize.setMaximumSize( cPanelDatasetSize.getPreferredSize( ) );
        cPanelTextPercent.setMaximumSize( cPanelTextPercent.getPreferredSize( ) );
        cPanelIsPhoto.setMaximumSize( cPanelIsPhoto.getPreferredSize( ) );
        cPanelComments.setMaximumSize( cPanelComments.getPreferredSize( ) );
        cPanelTagsCount.setMaximumSize( cPanelTagsCount.getPreferredSize( ) );
        cPanelTotalLikes.setMaximumSize( cPanelTotalLikes.getPreferredSize( ) );
        
        cPanelProperties.setMaximumSize( cPanelProperties.getPreferredSize( ) );
        cPanelLearning.setMaximumSize( cPanelLearning.getPreferredSize( ) );
        cPanelWhitespace.setMaximumSize( cPanelWhitespace.getPreferredSize( ) );
        
        //ds align right horizontally
        cPanelImageID.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelLikes.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelDislikes.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelVisits.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelConfidence.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelType.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelDatasetSize.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelTextPercent.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelIsPhoto.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelComments.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelTagsCount.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelTotalLikes.setAlignmentX( Component.RIGHT_ALIGNMENT );
        
        cPanelProperties.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelLearning.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelWhitespace.setAlignmentX( Component.RIGHT_ALIGNMENT );
        
        cPanelProperties.setBorder( BorderFactory.createLineBorder( Color.blue ) );
        cPanelLearning.setBorder( BorderFactory.createLineBorder( Color.blue ) );
        
        //ds north panels
        final JPanel cPanelTitle = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
        final JPanel cPanelURL   = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
        final JPanel cPanelTags  = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
        //final JPanel cPanelFlowWhitespace = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
        
        //ds connect
        /*cPanelTitle.add( new JLabel( " Title: " ) );*/ cPanelTitle.add( m_cTextFieldTitle );
        cPanelURL.add( new JLabel( "  URL: " ) );    cPanelURL.add( m_cTextFieldURL );
        cPanelTags.add( new JLabel( "Tags: " ) );   cPanelTags.add( m_cTextFieldTags );
        
        //ds setup the main panels
        m_cPanelNorth.setLayout( new BoxLayout( m_cPanelNorth, BoxLayout.Y_AXIS ) );
        m_cPanelNorth.setAlignmentX( Component.LEFT_ALIGNMENT );
        //m_cPanelNorth.add( cPanelURL );
        //m_cPanelNorth.add( cPanelTags );
        //m_cPanelNorth.add( cPanelFlowWhitespace );
        m_cPanelNorth.add( cPanelTitle );
        
        //ds side panel
        m_cPanelEast.setLayout( new BoxLayout( m_cPanelEast, BoxLayout.Y_AXIS ) );
        m_cPanelEast.add( cPanelProperties );
        m_cPanelEast.add( cPanelImageID );
        m_cPanelEast.add( cPanelType );
        m_cPanelEast.add( cPanelTextPercent );
        m_cPanelEast.add( cPanelIsPhoto );
        m_cPanelEast.add( cPanelLikes );
        m_cPanelEast.add( cPanelDislikes );
        m_cPanelEast.add( cPanelComments );
        m_cPanelEast.add( cPanelTagsCount );
        
        m_cPanelEast.add( cPanelWhitespace );
        
        m_cPanelEast.add( cPanelLearning );
        m_cPanelEast.add( cPanelDatasetSize );
        m_cPanelEast.add( cPanelVisits );
        m_cPanelEast.add( cPanelTotalLikes );
        m_cPanelEast.add( cPanelConfidence );
        
        //m_cPanelSouth.add( m_cButtonReset );
        m_cPanelSouth.add( m_cButtonPrevious );   
        m_cPanelSouth.add( m_cButtonDislike );
        m_cPanelSouth.add( m_cButtonLike ); 
    }
    
    //ds MySQL logger
    private final void _logMaster( final String p_strInfo )
    {
        //ds get username
        final String strUsername = m_cLearner.getUsername( );
        
        //ds if set
        if( null != strUsername )
        {
            try
            {
                //ds log
                m_cMySQLManager.logMaster( strUsername, p_strInfo );
            }
            catch( SQLException e )
            {
                System.out.println( "[" + CLogger.getStamp( ) + "]<CGUI>(_logMaster) SQLException: " + e.getMessage( ) + " could not log to MySQL master" );
            }
        }
        else
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CGUI>(_logMaster) could not log to master because of empty username" );
        }
    }

    //ds virtuals
    public void keyReleased( KeyEvent p_cKey ){ /*ds nothing to do*/ }
    public void keyTyped(KeyEvent p_cKey ){ /*ds nothing to do*/ }
}
