clear;

%ds font size
uFontSize = 16;

%ds load csv data
matCutoffs = csvread( 'growth.csv' );

%ds extract vectors
vecImageIDs = matCutoffs( :, 1 );
vecFeatures = matCutoffs( :, 2 );
vecTagTypes = matCutoffs( :, 3 );

%ds compute the tag density
vecRelative = vecTagTypes./vecImageIDs;
 
%ds plot
figure( 1 );
hPlot = plot( vecImageIDs, vecFeatures, vecImageIDs, vecTagTypes, vecImageIDs, vecImageIDs, '--' );
%axis( [0, vecImageID( end ), 0, 90 ] );
set(gca,'XTickLabel',num2str(get(gca,'XTick').'));
set(gca,'YTickLabel',num2str(get(gca,'YTick').'));
title( 'Data Growth', 'FontSize', uFontSize, 'interpreter', 'Latex' );
xLabel1 = xlabel( 'Image Number', 'FontSize', uFontSize, 'interpreter', 'Latex' );
yLabel1 = ylabel( 'Count', 'FontSize', uFontSize, 'interpreter', 'Latex' );

%ds fix label to axis spacing
set( xLabel1, 'Units', 'Normalized', 'Position', [ 0.5, -0.09, 0]);
set( yLabel1, 'Units', 'Normalized', 'Position', [-0.13,  0.5, 0]);

hLegend = legend( 'Features', 'Tags', 'Images' );
set( hLegend, 'interpreter', 'Latex' );
grid on;

set( gca, 'FontSize', uFontSize );

saveas( 1, 'data_growth.eps', 'epsc' );

%ds plot
figure( 2 );
plot( vecImageIDs, vecRelative, 'blue' );
%axis( [0, vecImageID( end ), 0, iMaximumPoints ] );
%set(gca,'YTickLabel',num2str(get(gca,'YTick').'));
grid on;
set(gca,'XTickLabel',num2str(get(gca,'XTick').'));
title( 'Tag Density Graph', 'FontSize', uFontSize, 'interpreter', 'Latex' );
xLabel2 = xlabel( 'Image Number', 'FontSize', uFontSize, 'interpreter', 'Latex' );
yLabel2 = ylabel( 'Tag Density', 'FontSize', uFontSize, 'interpreter', 'Latex' );

%ds fix label to axis spacing
set( xLabel2, 'Units', 'Normalized', 'Position', [ 0.5, -0.09, 0]);
set( yLabel2, 'Units', 'Normalized', 'Position', [-0.08,  0.5, 0]);

set( gca, 'FontSize', uFontSize );

saveas( 2, 'tag_density.eps', 'epsc' );
