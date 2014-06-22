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
%matLearning9 = csvread( 'users/derPekka.csv' );
%matLearning10 = csvread( 'users/grosser general.csv' );
matLearning11 = csvread( 'users/labamba.csv' );
matLearning12 = csvread( 'users/proud zep user.csv' );

%ds plot probability curves
plotFigureWithoutRandomPoints( matLearning1, 'ChillyOnMyWilly', 'users/probability_curve_ChillyOnMyWilly.eps' );
plotFigureWithoutRandomPoints( matLearning3, 'Isabellinska', 'users/probability_curve_Isabellinska.eps' );
plotFigureWithoutRandomPoints( matLearning6, 'ProProcrastinator', 'users/probability_curve_ProProcrastinator.eps' );
plotFigureWithoutRandomPoints( matLearning7, 'themanuuu', 'users/probability_curve_themanuuu.eps' );
plotFigureWithoutRandomPoints( matLearning8, 'memyself', 'users/probability_curve_memyself.eps' );

%ds session 2
%plotFigureWithoutRandomPoints( matLearning9, 'derPekka', 'users/probability_curve_derPekka.eps' );
%plotFigureWithoutRandomPoints( matLearning10, 'grosser general', 'users/probability_curve_grosser general.eps' );
plotFigureWithoutRandomPoints( matLearning11, 'labamba', 'users/probability_curve_labamba.eps' );
plotFigureWithoutRandomPoints( matLearning12, 'proud zep user', 'users/probability_curve_proud zep user.eps' );
