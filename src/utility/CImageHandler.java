package utility;

import java.awt.image.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.imgscalr.Scalr;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

//ds custom
import exceptions.CZEPConversionException;

public abstract class CImageHandler
{
    public final static BufferedImage getResizedImage( BufferedImage p_cImage, final int p_iWindowWidth, final int p_iWindowHeight )
    {
        //ds get size information
        final int iWidthImage  = p_cImage.getWidth( );
        final int iHeightImage = p_cImage.getHeight( );
        
        //ds get ratios
        final double dRatioWidth  = ( double ) iWidthImage/p_iWindowWidth;
        final double dRatioHeight = ( double ) iHeightImage/p_iWindowHeight;
        
        //ds don't scale for ratios above 3.0
         if( 3.0 < dRatioWidth || 3.0 < dRatioHeight ){ return p_cImage; }
        
        //ds check which side we have to scale
        if( dRatioWidth > dRatioHeight )
        {
            //ds scale image to width
            p_cImage = Scalr.resize( p_cImage, Scalr.Mode.FIT_TO_WIDTH, p_iWindowWidth, p_iWindowHeight );                
        }
        else
        {
            //ds scale image to height
            p_cImage = Scalr.resize( p_cImage, Scalr.Mode.FIT_TO_HEIGHT, p_iWindowWidth, p_iWindowHeight );  
        }
        
        return p_cImage;
    }
    
    //ds compute text percentage
    public final static double getTextPercentageCanny( final BufferedImage p_cImage ) throws IOException, CZEPConversionException
    {    	
    	//ds allocate image instances (all in grayscale)
    	Mat matImageRGB       = _getMatFromBufferedImage( p_cImage );
    	Mat matImageGrayScale = Mat.zeros( matImageRGB.size( ), CvType.CV_8UC1 );
    	
        //ds get the grayscale representation
        Imgproc.cvtColor( matImageRGB, matImageGrayScale, Imgproc.COLOR_RGB2GRAY );
        
        //ds tophat filter 
        Imgproc.morphologyEx( matImageGrayScale, matImageGrayScale, Imgproc.MORPH_TOPHAT, Imgproc.getStructuringElement( Imgproc.CV_SHAPE_RECT, new Size( 21, 3 ) ) );

        //ds threshold
        Imgproc.threshold( matImageGrayScale, matImageGrayScale, 125, 255, Imgproc.THRESH_BINARY );
        
        //ds smooth the image
        Imgproc.GaussianBlur( matImageGrayScale, matImageGrayScale, new Size( 3, 3 ), 0 );
        
        //ds dilate the structure
        Imgproc.dilate( matImageGrayScale, matImageGrayScale, Imgproc.getStructuringElement( Imgproc.CV_SHAPE_RECT, new Size( 20, 3 ) ) );
        
        //ds threshold again
        Imgproc.threshold( matImageGrayScale, matImageGrayScale, 150, 255, Imgproc.THRESH_BINARY );
        
        //ds execute canny edge detection
        Imgproc.Canny( matImageGrayScale, matImageGrayScale, 500, 1000 );
        
        //ds allocate mat list
        List< MatOfPoint > vecContours = new ArrayList< MatOfPoint >( );
        
        //ds find the contours
        Imgproc.findContours( matImageGrayScale, vecContours, new Mat( ), Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE );
        
        //ds rectangle vector
        Vector< Rect > vecTextBoxes = new Vector< Rect >( );
        
        //ds get the rectangles from the contours and check them
        for( MatOfPoint matPoints : vecContours )
        {
        	//ds add the box
        	vecTextBoxes.add( Imgproc.boundingRect( matPoints ) );	
        }
        
        //ds if we dont have any boxes we can return here
        if( vecTextBoxes.isEmpty( ) )
        {
        	//ds no text detected
        	return 0;
        }
        
        //ds remove double boxes
        vecTextBoxes = _removeDoubleBoxes( vecTextBoxes );
        
        //ds now fix bad boxes and connect them (contours connected to the border of the image, split up)
    	vecTextBoxes = _removeInnerBoxes( vecTextBoxes );
    	
    	//ds fix border boxes
    	vecTextBoxes = _fixBorderBoxes( vecTextBoxes, 10 );

    	//ds remove squared or vertical boxes (no text assumed)
    	vecTextBoxes = _removeVerticalBoxes( vecTextBoxes, 1.5 );
    	
    	//ds remove small boxes
    	vecTextBoxes = _removeSmallBoxes( vecTextBoxes, 500 );	

        //ds total area
        double dTotalTextArea = 0.0;

        //ds loop over the boxes
        for( Rect cTextBox : vecTextBoxes )
        {
        	//ds add to total area
        	dTotalTextArea += cTextBox.area( );
        	
        	/*ds compute corner points for the rectangle to draw
        	final Point cBR = new Point( cTextBox.x, cTextBox.y );
        	final Point cTL = new Point( cTextBox.x+cTextBox.width, cTextBox.y+cTextBox.height );
        
        	//ds and draw it on the input matrix
        	Core.rectangle( matImageRGB, cBR, cTL, new Scalar( 255, 0, 0 ), 2 );*/
        }
        
        //ds final image
        //Highgui.imwrite( "final.jpg", matImageRGB );
        
        //ds compute text percentage
        return dTotalTextArea/matImageRGB.total( );
    }
    
    //ds check if the image is a photograph or a drawing
    public final static boolean isAPhotographRGB( final BufferedImage p_cImage ) throws CZEPConversionException
    {
    	//ds matrices for histogram
    	List< Mat > lstImageRGB = new ArrayList< Mat >( );
    	
    	//ds allocate image instance
    	lstImageRGB.add( _getMatFromBufferedImage( p_cImage ) );
    	
    	//ds histogram configuration
    	final int iHistSize         = 256;
    	final MatOfInt arrChannelsR = new MatOfInt( 0 );
    	final MatOfInt arrChannelsG = new MatOfInt( 1 );
    	final MatOfInt arrChannelsB = new MatOfInt( 2 );
    	final MatOfInt arrHistSize  = new MatOfInt( iHistSize );
    	final MatOfFloat arrRanges  = new MatOfFloat( 0, 255 );
    	
    	//ds histograms
    	Mat matHistogramR = new Mat( );
    	Mat matHistogramG = new Mat( );
    	Mat matHistogramB = new Mat( );
    	
    	//ds get a histogram from the image
    	Imgproc.calcHist( lstImageRGB, arrChannelsR, new Mat( ), matHistogramR, arrHistSize, arrRanges, true );
    	Imgproc.calcHist( lstImageRGB, arrChannelsG, new Mat( ), matHistogramG, arrHistSize, arrRanges, true );
    	Imgproc.calcHist( lstImageRGB, arrChannelsB, new Mat( ), matHistogramB, arrHistSize, arrRanges, true );
    	
    	//ds histogram dimensions
    	final int iHistWidth  = 800;
    	final int iHistHeight = 600;
    	
    	//ds width per bin
    	final int iBinWidth  = ( int ) Math.round( ( double ) iHistWidth/iHistSize );

    	//ds allocate the final image
    	Mat matHistogramRGB = new Mat( iHistHeight, iHistWidth, CvType.CV_8UC3, new Scalar( 0, 0, 0 ) );
    	
    	System.out.println( matHistogramR );
    	
    	//ds count all entries
    	int iNumberOfEntriesR = 0;
    	int iNumberOfEntriesG = 0;
    	int iNumberOfEntriesB = 0;
    	
    	//ds check for a max value
    	double dMaxValueR = 0;
    	double dMaxValueG = 0;
    	double dMaxValueB = 0;
    	
    	//ds plot values
    	for( int i = 0; i < iHistSize; ++i )
    	{
    		//ds get current entries
    		final double dCurrentEntriesR = matHistogramR.get( i, 0 )[0];
    		final double dCurrentEntriesG = matHistogramG.get( i, 0 )[0];
    		final double dCurrentEntriesB = matHistogramB.get( i, 0 )[0];
    		
    		//System.out.println( "R: " + i + " - " + dCurrentEntries );
    		
    		//ds check if bigger and update if so
    		if( dMaxValueR < dCurrentEntriesR ){ dMaxValueR = dCurrentEntriesR; }
    		if( dMaxValueG < dCurrentEntriesG ){ dMaxValueG = dCurrentEntriesG; }
    		if( dMaxValueB < dCurrentEntriesB ){ dMaxValueB = dCurrentEntriesB; }
    		
    		//ds increase entries
    		iNumberOfEntriesR += dCurrentEntriesR;
    		iNumberOfEntriesG += dCurrentEntriesG;
    		iNumberOfEntriesB += dCurrentEntriesB;
    	}
    	
    	System.out.println( "total 1: " + lstImageRGB.get( 0 ).width( )*lstImageRGB.get( 0 ).height( ) );
    	System.out.println( "total R: " + iNumberOfEntriesR );
    	System.out.println( "total G: " + iNumberOfEntriesG );
    	System.out.println( "total B: " + iNumberOfEntriesB );
    	System.out.println( "  max R: " + dMaxValueR/iNumberOfEntriesR );
    	System.out.println( "  max G: " + dMaxValueG/iNumberOfEntriesG );
    	System.out.println( "  max B: " + dMaxValueB/iNumberOfEntriesB );
    	
    	//ds compute average maximum value
    	final double dAverageMaximum = ( dMaxValueR/iNumberOfEntriesR + dMaxValueG/iNumberOfEntriesG + dMaxValueB/iNumberOfEntriesB )/3;

    	//ds normalize the result
    	Core.normalize( matHistogramR, matHistogramR, 0, matHistogramRGB.rows( ), Core.NORM_MINMAX, -1, new Mat( ) );
    	Core.normalize( matHistogramG, matHistogramG, 0, matHistogramRGB.rows( ), Core.NORM_MINMAX, -1, new Mat( ) );
    	Core.normalize( matHistogramB, matHistogramB, 0, matHistogramRGB.rows( ), Core.NORM_MINMAX, -1, new Mat( ) );

    	//ds raw for each channel
    	for( int i = 1; i < iHistSize; ++i )
    	{
    		//ds the curve
    		Core.line( matHistogramRGB,
    				   new Point( iBinWidth*( i-1 ), iHistHeight - Math.round( matHistogramR.get( i-1,0 )[0] ) ),
                       new Point( iBinWidth*( i )  , iHistHeight - Math.round( matHistogramR.get( i, 0 )[0] ) ),
                       new Scalar( 0, 0, 255 ), 1 );
    		Core.line( matHistogramRGB,
 				   	   new Point( iBinWidth*( i-1 ), iHistHeight - Math.round( matHistogramG.get( i-1,0 )[0] ) ),
 				   	   new Point( iBinWidth*( i )  , iHistHeight - Math.round( matHistogramG.get( i, 0 )[0] ) ),
 				   	   new Scalar( 0, 255, 0 ), 1 );
    		Core.line( matHistogramRGB,
 				   	   new Point( iBinWidth*( i-1 ), iHistHeight - Math.round( matHistogramB.get( i-1,0 )[0] ) ),
 				   	   new Point( iBinWidth*( i )  , iHistHeight - Math.round( matHistogramB.get( i, 0 )[0] ) ),
 				   	   new Scalar( 255, 0, 0 ), 1 );
    	}
    	
    	//ds save the image
    	Highgui.imwrite( "histogram.jpg", matHistogramRGB );
    	
    	//ds return
    	return ( dAverageMaximum < 0.1 );
    }
    
    //ds check if the image is a photograph or a drawing
    public final static boolean isAPhotographGray( final BufferedImage p_cImage, final double p_dThreshold ) throws CZEPConversionException
    {
    	//ds allocate image instances
    	Mat matImageRGB       = _getMatFromBufferedImage( p_cImage );
    	Mat matImageGrayScale = Mat.zeros( matImageRGB.size( ), CvType.CV_8UC1 );
    	//Mat MatImageGradient  = new Mat( matImageRGB.size( ), CvType.CV_32FC1 );
    	
        //ds set the grayscale representation
        Imgproc.cvtColor( matImageRGB, matImageGrayScale, Imgproc.COLOR_RGB2GRAY );
        
        //ds compute the gradient image
        //Imgproc.Sobel( matImageGrayScale, MatImageGradient, CvType.CV_32F, 1, 1 );
        
        //ds info
        //Highgui.imwrite( "grayscale.jpg", matImageGrayScale );
    	//Highgui.imwrite( "gradient.jpg", MatImageGradient );
        
    	//ds matrices for histogram
    	final List< Mat > lstImageGray = new ArrayList< Mat >( );
    	
    	//ds get a list
    	lstImageGray.add( matImageGrayScale );
    	
    	//ds histogram configuration
    	final int iHistSize        = 256;
    	final MatOfInt arrChannels = new MatOfInt( 0 );
    	final MatOfInt arrHistSize = new MatOfInt( iHistSize );
    	final MatOfFloat arrRanges = new MatOfFloat( 0, 255 );
    	
    	//ds raw histogram data
    	Mat matHistogramGrayRaw = new Mat( );
    	
    	//ds get a histogram from the image
    	Imgproc.calcHist( lstImageGray, arrChannels, new Mat( ), matHistogramGrayRaw, arrHistSize, arrRanges, true );
    	
    	//ds histogram drawing height
    	//final int iHistHeight = 600;
    	
    	//ds pixel width per bin
    	//final int iBinWidth = 3;
    	
    	//ds compute width
    	//final int iHistWidth = iHistSize*3;

    	//ds check for the max value
    	double dMaxValue = 0;
    	
    	//ds plot values
    	for( int i = 0; i < iHistSize; ++i )
    	{
    		//ds get current entries
    		final double dCurrentEntries = matHistogramGrayRaw.get( i, 0 )[0];
    		
    		//ds check if bigger and update
    		if( dMaxValue < dCurrentEntries ){ dMaxValue = dCurrentEntries; }
    	}
    	
    	//ds get std dev and mean
    	MatOfDouble matMean   = new MatOfDouble( 0.0 );
    	MatOfDouble matStdDev = new MatOfDouble( 0.0 );
    	Core.meanStdDev( matHistogramGrayRaw, matMean, matStdDev );
    	
    	//ds set statistical values
    	final double dTotalEntries = Core.sumElems( matHistogramGrayRaw ).val[0];
    	double dMean               = matMean.get( 0, 0 )[0];
    	double dStdDev             = matStdDev.get( 0, 0 )[0];
    	
    	//ds normalize all values
    	dMaxValue = dMaxValue/dTotalEntries;
    	dMean     = dMean/dTotalEntries;
    	dStdDev   = dStdDev/dTotalEntries;
    	
    	/*ds allocate the final image for drawing
    	Mat matHistogramGrayDraw = new Mat( iHistHeight, iHistWidth, CvType.CV_8UC1, new Scalar( 0, 0, 0 ) );

    	//ds normalize the result
    	Core.normalize( matHistogramGrayRaw, matHistogramGrayRaw, 0, matHistogramGrayDraw.rows( ), Core.NORM_MINMAX, -1, new Mat( ) );

    	//ds raw for each channel
    	for( int i = 1; i < iHistSize; ++i )
    	{
    		//ds the curve
    		Core.line( matHistogramGrayDraw,
    				   new Point( iBinWidth*( i-1 ), iHistHeight - Math.round( matHistogramGrayRaw.get( i-1,0 )[0] ) ),
                       new Point( iBinWidth*( i )  , iHistHeight - Math.round( matHistogramGrayRaw.get( i, 0 )[0] ) ),
                       new Scalar( 255, 0, 0 ), 2 );
    	}
    	
    	//ds save the image
    	Highgui.imwrite( "histogram.jpg", matHistogramGrayDraw );*/
    	
    	//ds return
    	return ( dMaxValue < p_dThreshold );
    }
    
    //ds convert from BufferedImage to Mat
    private final static Mat _getMatFromBufferedImage( final BufferedImage p_cImage ) throws CZEPConversionException
    {
    	//ds depending on type
    	if( BufferedImage.TYPE_3BYTE_BGR == p_cImage.getType( ) )
    	{
        	//ds get raw data
        	byte[] btRawData = ( ( DataBufferByte ) p_cImage.getRaster( ).getDataBuffer( ) ).getData( );
        	
	        //ds allocate a matrix
	        Mat matImage = new Mat( p_cImage.getHeight( ), p_cImage.getWidth( ), CvType.CV_8UC3 );
	        
	        //ds fill the raw data into the matrix
	        matImage.put( 0, 0, btRawData );
	
	        //ds return
	        return matImage;
    	}
    	else if( BufferedImage.TYPE_4BYTE_ABGR == p_cImage.getType( ) )
    	{
        	//ds get raw data
        	byte[] btRawData = ( ( DataBufferByte ) p_cImage.getRaster( ).getDataBuffer( ) ).getData( );
        	
	        //ds allocate a matrix
	        Mat matImage = new Mat( p_cImage.getHeight( ), p_cImage.getWidth( ), CvType.CV_8UC4 );
	        
	        //ds fill the raw data into the matrix
	        matImage.put( 0, 0, btRawData );
	
	        //ds return
	        return matImage;    		
    	}
    	else
    	{
    		throw( new CZEPConversionException( "could not transform BufferedImage to OpenCV Mat" ) );
    	}
    }
    
    //ds fixes border boxes (boxes originated from lines)
    private final static Vector< Rect > _fixBorderBoxes( final Vector< Rect > p_vecBoxes, final int p_iMaxHeight )
	{
    	//ds vector with border fixed boxes
    	Vector< Rect > vecTextBoxesBorderFixed = new Vector< Rect >( );
    	
        //ds now fix bad boxes (contours connected to the border of the image)
        for( int i = 0; i < p_vecBoxes.size( )-1; ++i )
        {
        	//ds get the current box and the next one (bad access checked)
        	final Rect cTextBox     = p_vecBoxes.get( i );
        	final Rect cTextBoxNext = p_vecBoxes.get( i+1 );
        	
        	//ds if we are at the border and have small height (almost a line)
        	if( 1 == cTextBox.x && p_iMaxHeight > cTextBox.height )
        	{
        		//ds and the next box fulfills the line criteria as well
        		if( 1 == cTextBoxNext.x && p_iMaxHeight > cTextBoxNext.height )
        		{
        			//ds we have to merge these to boxes and add it to the final vector
        			vecTextBoxesBorderFixed.add( new Rect( cTextBoxNext.x, cTextBoxNext.y, cTextBoxNext.width , cTextBox.y-cTextBoxNext.y ) );
        		}
        		else
        		{
        			//ds TODO: take care of boxes between the invalid boxes
        			//ds however this should not be the case at the point this function is called
        		}
        	}
        	else
        	{
        		//ds just add the box
        		vecTextBoxesBorderFixed.add( cTextBox );
        	}
        }
        
        //ds always add the last box if its valid
        if( p_iMaxHeight < p_vecBoxes.lastElement( ).height ){ vecTextBoxesBorderFixed.add( p_vecBoxes.lastElement( ) ); }
        
        //ds return
        return vecTextBoxesBorderFixed;
	}
    
    //ds remove double boxes (sometimes we have double contour information)
    private final static Vector< Rect > _removeDoubleBoxes( final Vector< Rect > p_vecBoxes )
    {
    	//ds loop over all boxes
        for( int i = 0; i < p_vecBoxes.size( )-1; ++i )
        {
        	//ds get the current and next box
        	final Rect cTextBox     = p_vecBoxes.get( i );
        	final Rect cTextBoxNext = p_vecBoxes.get( i+1 );
        	
        	//ds if they have identical values
        	if( cTextBox.x     == cTextBoxNext.x     && cTextBox.y      == cTextBoxNext.y      &&
        		cTextBox.width == cTextBoxNext.width && cTextBox.height == cTextBoxNext.height )
        	{
        		//ds remove it
        		p_vecBoxes.remove( i+1 );
        	}
        }
        
        //ds and return
        return p_vecBoxes;
    }
    
    //ds remove inner boxes
    private final static Vector< Rect > _removeInnerBoxes( final Vector< Rect > p_vecBoxes )
    {
    	//ds filtered boxes
    	Vector< Rect > vecBoxesClean = new Vector< Rect >( p_vecBoxes );
    	
        //ds loop over all boxes
        for( Rect cTextBox : p_vecBoxes )
        {
        	//ds loop over all other boxes
            for( int i = 0; i < vecBoxesClean.size( ); ++i )
            {
	        	//ds get the current box
	        	final Rect cTextBoxNext = vecBoxesClean.get( i );
	        	
	        	//ds if its not the same as the outer one
	        	if( cTextBoxNext != cTextBox )
	        	{
		        	//ds check if the next box could possible fit in the current one
		        	if( cTextBox.height > cTextBoxNext.height && cTextBox.width > cTextBoxNext.width )
		        	{
		        		//ds get relative corner distances
		        		final int iPixelDistanceX = cTextBoxNext.x-cTextBox.x;
		        		final int iPixelDistanceY = cTextBoxNext.y-cTextBox.y;
		        		
		        		//ds check the position
		        		if( 0 < iPixelDistanceX && cTextBox.width  > iPixelDistanceX &&
		        			0 < iPixelDistanceY && cTextBox.height > iPixelDistanceY )
		        		{
		        			//ds remove the box
		        			vecBoxesClean.remove( i );
		        		}
		        	}
		        	else
		        	{
		        		//ds get the id of the outer box
		        		final int iBox = vecBoxesClean.indexOf( cTextBox );
		        		
		        		//ds if its still in the vector
		        		if( -1 != iBox )
		        		{
			        		//ds check if the outer box fits in the inner
			        		if( cTextBox.height < cTextBoxNext.height && cTextBox.width < cTextBoxNext.width )
			        		{
				        		//ds get relative corner distances
				        		final int iPixelDistanceX = cTextBox.x-cTextBoxNext.x;
				        		final int iPixelDistanceY = cTextBox.y-cTextBoxNext.y;
				        		
				        		//ds check the position
				        		if( 0 < iPixelDistanceX && cTextBoxNext.width  > iPixelDistanceX &&
				        			0 < iPixelDistanceY && cTextBoxNext.height > iPixelDistanceY )
				        		{
				        			//ds remove the box
				        			vecBoxesClean.remove( iBox );
				        		}
			        		}
		        		}
		        	}
	        	}
	        }
        }
        
        //ds return the clean vector
        return vecBoxesClean;
    }
  
    //ds remove vertical boxes
    private final static Vector< Rect > _removeVerticalBoxes( final Vector< Rect > p_vecBoxes, final double p_dFactor )
    {
    	//ds horizontal boxes
    	Vector< Rect > vecBoxesHorizontal = new Vector< Rect >( );    	
    	
    	//ds loop over all boxes
        for( Rect cTextBox : p_vecBoxes )
        {
        	//ds check if the box is horizontal
        	if( cTextBox.width > p_dFactor*cTextBox.height )
        	{
        		//ds add it
        		vecBoxesHorizontal.add( cTextBox );
        	}
        }
        
        //ds and return
        return vecBoxesHorizontal;
    }
    
    //ds remove small boxes
    private final static Vector< Rect > _removeSmallBoxes( final Vector< Rect > p_vecBoxes, final double p_dBoxMinimumArea )
    {
    	//ds horizontal boxes
    	Vector< Rect > vecBoxesFinal = new Vector< Rect >( );    	
    	
    	//ds loop over all boxes
        for( Rect cTextBox : p_vecBoxes )
        {
        	//ds check if the box has minimum size
        	if( cTextBox.area( ) >= p_dBoxMinimumArea )
        	{
        		//ds add it
        		vecBoxesFinal.add( cTextBox );
        	}
        }
        
        //ds and return
        return vecBoxesFinal;
    }
}
