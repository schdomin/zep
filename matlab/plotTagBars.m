clear;

%ds font size
uFontSize = 16;

%ds load csv data
%matTagsLiked    = csvread( 'tags_liked.csv' );
%matTagsDisliked = csvread( 'tags_disliked.csv' );

%ds open files
fileID_Liked    = fopen( 'tags_liked.csv' );
fileID_Disliked = fopen( 'tags_disliked.csv' );

%ds extract information
cell_Liked    = textscan( fileID_Liked, '%s%f%u%u','delimiter',',' );
cell_Disliked = textscan( fileID_Disliked, '%s%f%u%u','delimiter',',' );

%ds close files
fclose( fileID_Liked );
fclose( fileID_Disliked );

%ds extract vectors
vecTagsNames_Liked            = cell_Liked{1};
vecRelativeFrequency_Liked    = cell_Liked{2};
vecFrequency_Liked            = cell_Liked{3};
vecFrequency_Liked_Total      = cell_Liked{4};
vecTagsNames_Disliked         = cell_Disliked{1};
vecRelativeFrequency_Disliked = cell_Disliked{2};
vecFrequency_Disliked         = cell_Disliked{3};
vecFrequency_Disliked_Total   = cell_Disliked{4};

%ds top 10
uMaximumElements = 10;

%ds plot
figure( 1 );
%subplot(3,1,1);

hPlot1 = bar( vecRelativeFrequency_Liked( 1:uMaximumElements ), 'FaceColor', [0 0.75 0] );

%ds draw absolute numbers on bars
for u = 1:1:uMaximumElements
    
    text( u, vecRelativeFrequency_Liked( u ), [ num2str( vecFrequency_Liked( u ) ), '/', num2str( vecFrequency_Liked_Total( u ) ) ], 'VerticalAlignment', 'bottom', 'HorizontalAlignment', 'center', 'FontSize', 12 );
    
end

set( gca( ), 'XTickLabel', vecTagsNames_Liked( 1:uMaximumElements ) );
rotateXLabels( gca( ), 45 );
set( gca( ), 'XLim', [0 uMaximumElements+1] );
set( gca( ), 'YLim', [0 0.55] );
set( hPlot1, 'barwidth', .75 );
hTitle1 = title( 'Top 10: Tags Liked', 'FontSize', uFontSize, 'interpreter', 'latex' );
yLabel1 = ylabel( 'Relative frequency [liked/available]', 'FontSize', uFontSize, 'interpreter', 'latex' );
hY1 = get( gca, 'YLabel' );
set( hY1, 'Units', 'Normalized' );
dPositionY1 = get( hY1, 'Position' );
set( hY1, 'Position', dPositionY1.*[ 1.1, 1, 1 ] );
set( yLabel1, 'Units', 'Normalized', 'Position', [-0.08,  0.5, 0]);
set( gca, 'FontSize' ,uFontSize );
saveas( 1, 'tags_liked.eps', 'epsc' );
saveas( 1, 'tags_liked.pdf' );

%ds plot
figure( 2 );
%subplot(3,1,3);
hPlot2 = bar( vecRelativeFrequency_Disliked( 1:uMaximumElements ), 'red' );

%ds draw absolute numbers on bars
for u = 1:1:uMaximumElements
   
    text( u, vecRelativeFrequency_Disliked( u ), [ num2str( vecFrequency_Disliked( u ) ), '/', num2str( vecFrequency_Disliked_Total( u ) ) ], 'VerticalAlignment', 'bottom', 'HorizontalAlignment', 'center', 'FontSize', 12 );
    
end

set( gca( ), 'XTickLabel', vecTagsNames_Disliked( 1:uMaximumElements ) );
rotateXLabels( gca( ), 45 );
set( gca( ), 'XLim', [0 uMaximumElements+1] );
set( gca( ), 'YLim', [0 0.55] );
set( hPlot2, 'barwidth', .75 );
hTitle2 = title( 'Top 10: Tags Disliked', 'FontSize', uFontSize, 'interpreter', 'latex' );
yLabel2 = ylabel( 'Relative frequency [disliked/available]', 'FontSize', uFontSize, 'interpreter', 'latex' );
hY2 = get( gca, 'YLabel' );
set( hY2, 'Units', 'Normalized' );
dPositionY2 = get( hY2, 'Position' );
set( hY2, 'Position', dPositionY2.*[ 1.1, 1, 1 ] );
set( yLabel2, 'Units', 'Normalized', 'Position', [-0.08,  0.5, 0]);
set( gca, 'FontSize' ,uFontSize );
saveas( 2, 'tags_disliked.eps', 'epsc' );
saveas( 2, 'tags_disliked.pdf' );

%saveas( 1, 'topten_tags.eps', 'epsc' );
