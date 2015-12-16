create table yagopreflabels as
    select A.subject,
           lower(substring(A.object from '"(.*)"')) as label,
           score    
    from yagofacts as A
            left join   
        (select subject, value as score 
         from yagofacts 
         where predicate = '<hasWikipediaArticleLength>') as B
            on A.subject = B.subject
    where (A.predicate = 'skos:prefLabel') and (A.object like '%@eng');
update yagopreflabels set score = 0 where score is null;

create index yagopreflabels_label_index on yagopreflabels(label);
create index yagopreflabels_score_index on yagopreflabels(score);