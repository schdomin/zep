%ds load csv data
%matTagsLiked    = csvread( 'tags_liked.csv' );
%matTagsDisliked = csvread( 'tags_disliked.csv' );

%ds open files
fileID_Liked    = fopen( 'tags_liked.csv' );
fileID_Disliked = fopen( 'tags_disliked.csv' );

%ds extract information
cell_Liked    = textscan( fileID_Liked, '%s%f','delimiter',',' );
cell_Disliked = textscan( fileID_Disliked, '%s%f','delimiter',',' );

%ds close files
fclose( fileID_Liked );
fclose( fileID_Disliked );

%ds extract vectors
vecTagsNames_Liked    = cell_Liked{1};
vecFrequency_Liked    = cell_Liked{2};
vecTagsNames_Disliked = cell_Disliked{1};
vecFrequency_Disliked = cell_Disliked{2};

%ds top 10
uMaximumElements = 10;

%ds plot
figure( 1 );
subplot(3,1,1);
hPlot1 = bar( vecFrequency_Liked( 1:uMaximumElements ) );
set( gca( ), 'XTickLabel', vecTagsNames_Liked( 1:uMaximumElements ) );
rotateXLabels( gca( ), 45 );
set( gca( ), 'XLim', [0 uMaximumElements+1] );
set( gca( ), 'YLim', [0 0.5] );
set( hPlot1, 'barwidth', .75 );
hTitle1 = title( 'Top 10: Tags Liked' );
ylabel( 'relative frequency [local/total]' );
hY1 = get( gca, 'YLabel' );
set( hY1, 'Units', 'Normalized' );
dPositionY1 = get( hY1, 'Position' );
set( hY1, 'Position', dPositionY1.*[ 1.1, 1, 1 ] );
%saveas( 1, 'tags_liked.pdf' );

%ds plot
subplot(3,1,3);
hPlot2 = bar( vecFrequency_Disliked( 1:uMaximumElements ) );
set( gca( ), 'XTickLabel', vecTagsNames_Disliked( 1:uMaximumElements ) );
rotateXLabels( gca( ), 45 );
set( gca( ), 'XLim', [0 uMaximumElements+1] );
set( gca( ), 'YLim', [0 0.5] );
set( hPlot2, 'barwidth', .75 );
hTitle2 = title( 'Top 10: Tags Disliked' );
ylabel( 'relative frequency [local/total]' );
hY2 = get( gca, 'YLabel' );
set( hY2, 'Units', 'Normalized' );
dPositionY2 = get( hY2, 'Position' );
set( hY2, 'Position', dPositionY2.*[ 1.1, 1, 1 ] );
saveas( 1, 'topten_tags.pdf' );
