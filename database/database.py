import csv
import pymysql

conn = pymysql.connect(
    host='localhost',
    user='root',
    password='1234',
    db='hr_erp',
    charset='utf8'
)
cursor = conn.cursor()

insert_sql = """
    INSERT INTO income_tax_table
    (salary_from, salary_to,
     fam_1, fam_2, fam_3, fam_4, fam_5, fam_6,
     fam_7, fam_8, fam_9, fam_10, fam_11, apply_year)
    VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s, 2026)
"""

with open('tax_bracket_db_ready.csv', 'r', encoding='utf-8') as f:
    reader = csv.DictReader(f)
    rows = []
    
    for row in reader:
        salary_from = int(row['MIN_PAY'])
        salary_to   = int(row['MAX_PAY'])
        
        # ✅ 문제 행 필터링
        if salary_to == 99999999999:
            continue
        
        rows.append((
            salary_from, salary_to,
            int(row['FAM_1']),  int(row['FAM_2']),
            int(row['FAM_3']),  int(row['FAM_4']),
            int(row['FAM_5']),  int(row['FAM_6']),
            int(row['FAM_7']),  int(row['FAM_8']),
            int(row['FAM_9']),  int(row['FAM_10']),
            int(row['FAM_11'])
        ))

    cursor.executemany(insert_sql, rows)
    conn.commit()
    print(f"✅ {cursor.rowcount}건 적재 완료")

cursor.close()
conn.close()