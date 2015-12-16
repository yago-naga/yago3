create table yagogeolabels as
	select distinct A.subject, A.label, C.score from 
	    (select subject, lower(substring(object from '"(.*)"')) as label
	     from yagofacts 
	     where predicate = 'rdfs:label' and object like '%@eng') as A
	            natural join
	    (select subject 
	     from yagofacts
	     where predicate = '<hasLatitude>') as B 
	            left join
	    (select subject, value as score 
	     from yagofacts 
	     where predicate = '<hasWikipediaArticleLength>') as C
	            on A.subject = C.subject;
	        
create index yagogeolabels_label_index on yagogeolabels(label);
create index yagogeolabels_score_index on yagogeolabels(score);
