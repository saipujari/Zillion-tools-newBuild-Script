select o.organization_name Practice, 
       o.id PracticeID, 
       to_char(s.CREATED_DT, 'MM-DD-YYYY') Enrollment_Date, 
       o.BILLING_ADDRESS Street, 
       o.BILLING_CITY City, 
       o.BILLING_ZIP ZipCode, 
       o.BILLING_STATE State, 
       o.BILLING_COUNTRY COUNTRY, 
       p.charge_Id TRANSACTION_ID,
       to_char(p.CREATED_DT, 'MM-DD-YYYY') "Bill Date",
       p.status TYPE,
       p.customer_receipt_number "Customer Receipt No", 
       m.PROGRAM_CODE PROGRAM_CODE, 
       m.PROGRAM_DESCRIPTION Program_Description,
       (CASE WHEN P.STATUS= 'Refunded' THEN -1 ELSE 1 END) UNIT,
      (p.amount - p.SALES_TAX_AMOUNT) / 100 Price, 
       p.SALES_TAX_AMOUNT / 100 sales_tax,
       p.amount / 100 TOTAL_AMOUNT,
       ' ' PO_NUMBER,
       'Credit Card' PAYMENT_TYPE
from organization o, 
     ACCOUNT a, 
     SUMMARY_ACCOUNT_TODATE s, 
     MP_A_MASTER_PROGRAM m, 
     PAYMENT_TRANSACTION p 
where PARENT_ORGANIZATION_IDS like '%0301%' 
      AND a.ORGANIZATION_ID = o.ID 
      AND s.ACCOUNT_ID=a.ID 
      AND s.MAST_PROGRAM_ID=m.id 
      AND a.EMAIL = p.USER_EMAIL 
     AND a.CREATED_DT BETWEEN TO_DATE ('09-28-2016', 'MM-DD-YYYY') 
      AND TO_DATE ('09-29-2016', 'MM-DD-YYYY') 
GROUP BY 
         o.id, 
         o.organization_name, 
         m.PROGRAM_DESCRIPTION, 
         s.CREATED_DT, 
         o.billing_city, 
	 o.billing_zip,
         o.billing_state, 
         o.billing_address, 
         o.billing_country, 
         p.sales_tax_amount, 
         p.CREATED_DT, 
         p.customer_receipt_number, 
         p.amount, 
         m.PROGRAM_CODE, 
         p.CREATED_DT,
         p.status,
         p.charge_ID
union ALL 
select o.organization_name Practice, 
       o.id PracticeID, 
       to_char(s.CREATED_DT, 'MM-DD-YYYY') Enrollment_Date, 
       o.BILLING_ADDRESS Street, 
       o.BILLING_CITY City, 
       o.BILLING_ZIP ZipCode, 
       o.BILLING_STATE State, 
       o.BILLING_COUNTRY COUNTRY, 
       ' ' TRANSACTION_ID,
       to_char(p.CREATED_DT, 'MM-DD-YYYY') "Bill Date", 
       p.status TYPE,
       p.customer_receipt_number "Invoice/Transaction No", 
       m.PROGRAM_CODE PROGRAM_CODE, 
       m.PROGRAM_DESCRIPTION Program_Description,
       (CASE WHEN P.STATUS= 'Refunded' THEN -1 ELSE 1 END) UNIT,
       (p.amount - 0) / 100 Price, 
       p.SALES_TAX_AMOUNT / 100 sales_tax,
       p.amount / 100 TOTAL_AMOUNT,
	     p.po_number PO_NUMBER,
       'INVOICE' PAYMENT_TYPE
from organization o, 
     ACCOUNT a, 
     SUMMARY_ACCOUNT_TODATE s, 
     MP_A_MASTER_PROGRAM m, 
     PAYMENT_INVOICE p 
where PARENT_ORGANIZATION_IDS like '%0301%' 
      AND a.ORGANIZATION_ID = o.ID 
      AND s.ACCOUNT_ID=a.ID 
      AND s.MAST_PROGRAM_ID=m.id 
      AND a.EMAIL = p.MEMBER_EMAIL 
      AND a.CREATED_DT BETWEEN TO_DATE ('09-28-2016', 'MM-DD-YYYY') 
      AND TO_DATE ('09-29-2016', 'MM-DD-YYYY') 
GROUP BY 
         o.id, 
         o.organization_name, 
         m.PROGRAM_DESCRIPTION, 
         s.CREATED_DT, 
         o.billing_city, 
	       o.billing_zip, 
         o.billing_state, 
         o.billing_address, 
         o.billing_country, 
         p.sales_tax_amount, 
         p.CREATED_DT, 
         p.customer_receipt_number, 
         p.amount, 
         m.PROGRAM_CODE,
         p.po_number,
         p.CREATED_DT,
         p.status;
