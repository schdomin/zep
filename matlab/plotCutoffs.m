clear;

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
title( 'Ratio f(x): $\frac{DataPoints(x)}{TagTypes(x)+x}$', 'interpreter', 'latex' );
xlabel( 'x: Cutoff frequency', 'interpreter', 'latex' );
ylabel( 'f(x): Ratio', 'interpreter', 'latex' );
grid on;

%ds snippet: get max and min values of the plot
xValue = get( hPlot,'XData' );
yValue = get( hPlot,'YData' );
imax = find( max( yValue ) == yValue );

%ds best cutoff
iBestCutoff = xValue( imax );

%ds label the max. and min. values  on the plot
text( xValue( imax ),yValue( imax ),[ ' $f_{max}=',num2str( yValue( imax ) ), '(x=',num2str( iBestCutoff ),')$' ],...
     'interpreter', 'latex', 'VerticalAlignment', 'bottom', 'HorizontalAlignment', 'left' );

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
line( [ iCoordinateX, iCoordinateX ], [0, iCoordinateY ], 'Color', 'green' );
axis( [0, vecCutoffs( end ), 0, iMaximumPoints ] );
set(gca,'YTickLabel',num2str(get(gca,'YTick').'));
grid on;
title( 'Cutoff Relation', 'interpreter', 'latex' );
xlabel( 'Cutoff frequency', 'interpreter', 'latex' );
ylabel( 'Amount', 'interpreter', 'latex' );
text( iCoordinateX, iCoordinateY, [ ' cutoff ', num2str( iBestCutoff ),': points=', num2str( iCoordinateY ), ', types=', num2str( iTypes ) ], 'interpreter', 'latex', 'VerticalAlignment', 'bottom', 'HorizontalAlignment', 'left' );
hLegend = legend( [ 'Tag types (total: ', num2str( iMaximumTypes ), ')\hspace{0.5cm}' ], [ 'Datapoints (total: ', num2str( iMaximumPoints ),  ')\hspace{0.5cm}' ], 'Current cutoff choice ($104$)' );
set( hLegend, 'interpreter', 'Latex' );

saveas( 2, 'cutoff_curve.eps', 'epsc' );
