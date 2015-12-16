create table yagolabels as
    select A.subject,
           lower(substring(A.object from '"(.*)"')) as label,
           score    
    from yagofacts as A
            left join
        (select subject, value as score 
         from yagofacts 
         where predicate = '<hasWikipediaArticleLength>') as B
            on A.subject = B.subject
    where (A.predicate = 'rdfs:label' or A.predicate = '<hasFamilyName>' or A.predicate = '<hasGivenName>') and A.object like '%@eng';

update yagolabels set score = 0 where score is null;

create index yagolabels_label_index on yagolabels(label);
create index yagolabels_score_index on yagolabels(score);