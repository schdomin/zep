clear;

%ds font size
uFontSize = 16;

%ds load csv data
matCutoffs = csvread( 'cutoffs.csv' );

%ds extract vectors
vecCutoffs  = matCutoffs( :, 1 );
vecTypes    = matCutoffs( :, 2 );
vecPoints   = matCutoffs( :, 3 );

%ds compute relative datapoints/tag types
vecRelative = vecPoints./( vecTypes+vecCutoffs );
 
%ds plot
figure( 1 );
hPlot = plot( vecCutoffs, vecRelative, 'blue' );
axis( [0, vecCutoffs( end ), 0, 90 ] );
title( 'Ratio $\eta(x)$: $\frac{Images(x)}{Tags(x)+x}$', 'FontSize', uFontSize, 'interpreter', 'latex' );
xLabel1 = xlabel( 'x: Cutoff frequency', 'FontSize', uFontSize, 'interpreter', 'latex' );
yLabel1 = ylabel( '$\eta(x)$: Ratio', 'FontSize', uFontSize, 'interpreter', 'latex' );

%ds fix label to axis spacing
set( xLabel1, 'Units', 'Normalized', 'Position', [ 0.5, -0.09, 0]);
set( yLabel1, 'Units', 'Normalized', 'Position', [-0.08,  0.5, 0]);

grid on;

%ds snippet: get max and min values of the plot
xValue = get( hPlot,'XData' );
yValue = get( hPlot,'YData' );
imax = find( max( yValue ) == yValue );

%ds best cutoff
iBestCutoff = xValue( imax );

%ds label the max. and min. values  on the plot
text( xValue( imax ),yValue( imax ),[ ' $\eta_{max}=',num2str( yValue( imax ) ), '(x=',num2str( iBestCutoff ),')$' ],...
     'FontSize', uFontSize, 'interpreter', 'latex', 'VerticalAlignment', 'bottom', 'HorizontalAlignment', 'left' );
set(gca,'FontSize',uFontSize);

saveas( 1, 'cutoff_choice.eps', 'epsc' );

%ds get max values
iMaximumTypes  = max( vecTypes );
iMaximumPoints = max( vecPoints );

%ds mark cutoff choice
iIndexCutoff = find( vecCutoffs==iBestCutoff );
iCoordinateX = vecCutoffs( iIndexCutoff );
iCoordinateY = vecPoints( iIndexCutoff );
iTypes       = vecTypes( iIndexCutoff );

%ds normalize
%vecNormalizedTypes = vecTypes./iMaximumTypes;
%vecNormalizedPoints = vecPoints./iMaximumPoints;

%ds plot
figure( 2 );
plot( vecCutoffs, vecTypes, 'blue', vecCutoffs, vecPoints, 'red' );
line( [ iCoordinateX, iCoordinateX ], [0, iCoordinateY ], 'Color', 'black', 'LineStyle', '--' );
axis( [0, vecCutoffs( end ), 0, iMaximumPoints ] );
set(gca,'YTickLabel',num2str(get(gca,'YTick').'));
grid on;
title( 'Cutoff Relation', 'FontSize', uFontSize, 'interpreter', 'latex' );
xLabel2 = xlabel( 'Cutoff frequency', 'FontSize', uFontSize, 'interpreter', 'latex' );
yLabel2 = ylabel( 'Number of Images', 'FontSize', uFontSize, 'interpreter', 'latex' );

%ds fix label to axis spacing
set( xLabel2, 'Units', 'Normalized', 'Position', [ 0.5, -0.09, 0]);
set( yLabel2, 'Units', 'Normalized', 'Position', [-0.13,  0.5, 0]);

text( iCoordinateX, iCoordinateY, [ ' cutoff ', num2str( iBestCutoff ),': images=', num2str( iCoordinateY ), ', tags=', num2str( iTypes ) ], 'FontSize', uFontSize, 'interpreter', 'latex', 'VerticalAlignment', 'bottom', 'HorizontalAlignment', 'left' );
hLegend = legend( [ 'Tags (total: ', num2str( iMaximumTypes ), ')\hspace{0.5cm}' ], [ 'Images (total: ', num2str( iMaximumPoints ),  ')\hspace{0.5cm}' ], 'Current cutoff choice ($104$)\hspace{0.1cm}' );
set( hLegend, 'interpreter', 'Latex' );
set(gca,'FontSize',uFontSize);

saveas( 2, 'cutoff_curve.eps', 'epsc' );
