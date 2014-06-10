clear;

%ds load csv data
matLearning1 = csvread( 'users/ChillyOnMyWilly.csv' );
matLearning2 = csvread( 'users/Glaus.csv' );
matLearning3 = csvread( 'users/Isabellinska.csv' );
matLearning4 = csvread( 'users/judas.csv' );
matLearning5 = csvread( 'users/juitto.csv' );
matLearning6 = csvread( 'users/ProProcrastinator.csv' );
matLearning7 = csvread( 'users/themanuuu.csv' );
matLearning8 = csvread( 'users/memyself.csv' );
matLearning9 = csvread( 'users/derPekka.csv' );
matLearning10 = csvread( 'users/grosser general.csv' );
matLearning11 = csvread( 'users/labamba.csv' );
matLearning12 = csvread( 'users/proud zep user.csv' );

%ds plot figures
plotFigureWithRandomPoints( matLearning1, 'ChillyOnMyWilly'  , 'users/probability_curve_ChillyOnMyWilly_random.eps' );
plotFigureWithRandomPoints( matLearning3, 'Isabellinska'     , 'users/probability_curve_Isabellinska_random.eps' );
plotFigureWithRandomPoints( matLearning6, 'ProProcrastinator', 'users/probability_curve_ProProcrastinator_random.eps' );
plotFigureWithRandomPoints( matLearning7, 'themanuuu'        , 'users/probability_curve_themanuuu_random.eps' );
plotFigureWithRandomPoints( matLearning8, 'memyself'         , 'users/probability_curve_memyself_random.eps' );

%ds session 2
%plotFigureWithRandomPoints( matLearning9, 'derPekka', 'users/probability_curve_derPekka.eps' );
%plotFigureWithRandomPoints( matLearning10, 'grosser general', 'users/probability_curve_grosser general.eps' );
plotFigureWithRandomPoints( matLearning11, 'labamba', 'users/probability_curve_labamba_random.eps' );
plotFigureWithRandomPoints( matLearning12, 'proud zep user', 'users/probability_curve_proud zep user_random.eps' );
