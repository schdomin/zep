clear;

%ds load csv data
matCutoffs = csvread( 'cutoffs.csv' );

%ds extract vectors
vecCutoffs  = matCutoffs( :, 1 );
vecTypes    = matCutoffs( :, 2 );
vecPoints   = matCutoffs( :, 3 );

%ds get max values
iMaximumTypes  = max( vecTypes );
iMaximumPoints = max( vecPoints );

%ds normalize
%vecNormalizedTypes = vecTypes./iMaximumTypes;
%vecNormalizedPoints = vecPoints./iMaximumPoints;

%ds plot
figure( 1 );
plot( vecCutoffs, vecTypes, 'blue', vecCutoffs, vecPoints, 'red' );
set(gca,'YTickLabel',num2str(get(gca,'YTick').'));
grid on;
title( 'Cutoff Relation', 'interpreter', 'latex' );
xlabel( 'Cutoff frequency', 'interpreter', 'latex' );
ylabel( 'Amount', 'interpreter', 'latex' );
hLegend = legend( [ 'Tag types (total: ', num2str( iMaximumTypes ), ')\hspace{0.5cm}' ], [ 'Datapoints (total: ', num2str( iMaximumPoints ),  ')\hspace{0.5cm}' ] );
set( hLegend, 'interpreter', 'Latex' );
saveas( 1, 'cutoffs.pdf' );

%ds compute relative datapoints/tag types
vecRelative = vecPoints./( vecTypes+vecCutoffs );
 
%ds plot
figure( 2 );
hPlot = plot( vecCutoffs, vecRelative, 'blue' );
title( 'Ratio f(x): $\frac{DataPoints(x)}{TagTypes(x)+x}$', 'interpreter', 'latex' );
xlabel( 'x: Cutoff frequency', 'interpreter', 'latex' );
ylabel( 'f(x): Ratio', 'interpreter', 'latex' );

%ds snippet: get max and min values of the plot
xValue = get( hPlot,'XData' );
yValue = get( hPlot,'YData' );
imax = find( max( yValue ) == yValue );

%ds label the max. and min. values  on the plot
text( xValue( imax ),yValue( imax ),[ ' $f_{max}=',num2str( yValue( imax ) ), '(x=',num2str( xValue( imax ) ),')$' ],...
     'interpreter', 'latex', 'VerticalAlignment', 'bottom', 'HorizontalAlignment', 'left' );

saveas( 2, 'cutoffs_relative.pdf' );