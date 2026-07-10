import os
import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore
import pandas as pd
from datetime import datetime

key_path = "firebase-key.json"
if not os.path.exists(key_path):
    print("\nERROR: firebase-key.json is missing!")
    print("Please place the firebase-key.json file in this directory and run again.")
    sys.exit(1)

# Initialize Firebase
cred = credentials.Certificate(key_path)
firebase_admin.initialize_app(cred)
db = firestore.client()

uid = "0xeS6f57bOUwFps3oW6Y9FcVzct1"
files_ref = db.collection("files").document(uid).collection("loan_files")

def safe_datetime_str(timestamp):
    if not timestamp:
        return ""
    try:
        if hasattr(timestamp, 'timestamp'):
            return timestamp.strftime('%Y-%m-%d %H:%M:%S')
        val = float(timestamp)
        if val > 0:
            return datetime.fromtimestamp(val / 1000.0).strftime('%Y-%m-%d %H:%M:%S')
    except Exception:
        pass
    return str(timestamp)

files_list = []
customers_list = []
transactions_list = []
files_map = {}
file_data_map = {}

print("Fetching data from Firestore using collection group queries (fast)...")

# 1. Fetch files
try:
    for file_doc in files_ref.stream():
        file_data = file_doc.to_dict()
        file_id = file_data.get("id") or file_doc.id
        file_name = file_data.get("name", "Unnamed File")
        created_at = file_data.get("createdAt")
        is_deleted = file_data.get("isDeleted", False) or file_data.get("deleted", False)
        
        file_date_str = safe_datetime_str(created_at)
        
        files_list.append({
            "File ID": file_id,
            "File Name": file_name,
            "Created At": file_date_str,
            "Is Deleted": is_deleted
        })
        files_map[str(file_doc.id)] = file_name
except Exception as e:
    print(f"Error fetching files: {e}")

# 2. Fetch persons and customers across all collections
persons_list = []
persons_map = {} # doc_id -> person_entry dict
try:
    # Query persons collection group
    for doc in db.collection_group("persons").stream():
        path_parts = doc.reference.path.split('/')
        if len(path_parts) >= 6 and path_parts[1] == uid:
            file_id = path_parts[3]
            file_name = files_map.get(file_id, "Unnamed File")
            
            person_data = doc.to_dict()
            person_id = person_data.get("id") or doc.id
            person_name = person_data.get("name", "")
            mobile = person_data.get("mobileNumber") or person_data.get("phone") or person_data.get("phoneNumber") or ""
            place = person_data.get("place") or person_data.get("address") or ""
            notes = person_data.get("notes") or ""
            p_created_at = person_data.get("createdAt")
            p_deleted = person_data.get("isDeleted", False) or person_data.get("deleted", False)
            
            p_date_str = safe_datetime_str(p_created_at)
            
            entry = {
                "File ID": file_id,
                "File Name": file_name,
                "Customer ID": person_id,
                "Name": person_name,
                "Mobile": mobile,
                "Place": place,
                "Notes": notes,
                "Created At": p_date_str,
                "Is Deleted": p_deleted,
                "_raw_doc_id": doc.id,
                "_raw_data": person_data
            }
            persons_list.append(entry)
            persons_map[doc.id] = entry

    # Query customers collection group
    for doc in db.collection_group("customers").stream():
        path_parts = doc.reference.path.split('/')
        if len(path_parts) >= 6 and path_parts[1] == uid:
            file_id = path_parts[3]
            file_name = files_map.get(file_id, "Unnamed File")
            
            person_data = doc.to_dict()
            person_id = person_data.get("id") or doc.id
            person_name = person_data.get("name", "")
            mobile = person_data.get("mobileNumber") or person_data.get("phone") or person_data.get("phoneNumber") or ""
            place = person_data.get("place") or person_data.get("address") or ""
            notes = person_data.get("notes") or ""
            p_created_at = person_data.get("createdAt")
            p_deleted = person_data.get("isDeleted", False) or person_data.get("deleted", False)
            
            p_date_str = safe_datetime_str(p_created_at)
            
            entry = {
                "File ID": file_id,
                "File Name": file_name,
                "Customer ID": person_id,
                "Name": person_name,
                "Mobile": mobile,
                "Place": place,
                "Notes": notes,
                "Created At": p_date_str,
                "Is Deleted": p_deleted,
                "_raw_doc_id": doc.id,
                "_raw_data": person_data
            }
            persons_list.append(entry)
            persons_map[doc.id] = entry
            
except Exception as e:
    print(f"Error fetching persons/customers: {e}")

# 3. Fetch loans (New Schema)
loans_map = {} # loan_doc_id -> loan details dict
persons_with_new_loans = set()
try:
    for doc in db.collection_group("loans").stream():
        path_parts = doc.reference.path.split('/')
        if len(path_parts) >= 8 and path_parts[1] == uid:
            file_id = path_parts[3]
            file_name = files_map.get(file_id, "Unnamed File")
            person_doc_id = path_parts[5]
            
            person_entry = persons_map.get(person_doc_id, {})
            person_name = person_entry.get("Name", "")
            place = person_entry.get("Place", "")
            
            loan_data = doc.to_dict()
            loan_id = loan_data.get("id") or doc.id
            amount = loan_data.get("totalAmount") or loan_data.get("amount") or loan_data.get("amountGiven") or 0.0
            date_given = loan_data.get("dateGiven") or loan_data.get("date") or loan_data.get("createdAt")
            l_deleted = loan_data.get("isDeleted", False) or loan_data.get("deleted", False)
            
            l_date_str = safe_datetime_str(date_given)
            
            persons_with_new_loans.add(person_doc_id)
            loans_map[doc.id] = {
                "File ID": file_id,
                "File Name": file_name,
                "Customer Name": person_name,
                "Place": place,
                "Loan ID": loan_id
            }
            
            transactions_list.append({
                "File ID": file_id,
                "File Name": file_name,
                "Customer Name": person_name,
                "Place": place,
                "Type": "GIVEN (Loan)",
                "Amount (Rs)": amount,
                "Date": l_date_str,
                "Transaction ID": loan_id,
                "Is Deleted": l_deleted
            })
except Exception as e:
    print(f"Error fetching loans: {e}")

# 4. Fetch payments and transactions
try:
    seen_payment_ids = set()
    # Payments Group
    for doc in db.collection_group("payments").stream():
        path_parts = doc.reference.path.split('/')
        if len(path_parts) >= 8 and path_parts[1] == uid:
            if doc.id in seen_payment_ids:
                continue
            seen_payment_ids.add(doc.id)
            
            file_id = path_parts[3]
            file_name = files_map.get(file_id, "Unnamed File")
            
            pay_data = doc.to_dict()
            pay_id = pay_data.get("id") or doc.id
            pay_amount = pay_data.get("amount") or pay_data.get("value") or pay_data.get("amountPaid") or 0.0
            pay_date = pay_data.get("date") or pay_data.get("timestamp") or pay_data.get("createdAt")
            pay_deleted = pay_data.get("isDeleted", False) or pay_data.get("deleted", False)
            
            pay_date_str = safe_datetime_str(pay_date)
            
            if len(path_parts) == 10:
                # New Schema: files/{uid}/loan_files/{file_id}/persons/{person_id}/loans/{loan_id}/payments/{payment_id}
                loan_id = path_parts[7]
                loan_info = loans_map.get(loan_id, {})
                person_name = loan_info.get("Customer Name", "")
                place = loan_info.get("Place", "")
            else:
                # Legacy Schema: files/{uid}/loan_files/{file_id}/persons/{person_id}/payments/{payment_id}
                person_doc_id = path_parts[5]
                person_entry = persons_map.get(person_doc_id, {})
                person_name = person_entry.get("Name", "")
                place = person_entry.get("Place", "")
                
            transactions_list.append({
                "File ID": file_id,
                "File Name": file_name,
                "Customer Name": person_name,
                "Place": place,
                "Type": "RECEIVED (Payment)",
                "Amount (Rs)": pay_amount,
                "Date": pay_date_str,
                "Transaction ID": pay_id,
                "Is Deleted": pay_deleted
            })

    # Transactions Group (in case some are in 'transactions' collection)
    for doc in db.collection_group("transactions").stream():
        path_parts = doc.reference.path.split('/')
        if len(path_parts) >= 8 and path_parts[1] == uid:
            if doc.id in seen_payment_ids:
                continue
            seen_payment_ids.add(doc.id)
            
            file_id = path_parts[3]
            file_name = files_map.get(file_id, "Unnamed File")
            
            pay_data = doc.to_dict()
            pay_id = pay_data.get("id") or doc.id
            pay_amount = pay_data.get("amount") or pay_data.get("value") or pay_data.get("amountPaid") or 0.0
            pay_date = pay_data.get("date") or pay_data.get("timestamp") or pay_data.get("createdAt")
            pay_deleted = pay_data.get("isDeleted", False) or pay_data.get("deleted", False)
            
            pay_date_str = safe_datetime_str(pay_date)
            
            if len(path_parts) == 10:
                # New Schema
                loan_id = path_parts[7]
                loan_info = loans_map.get(loan_id, {})
                person_name = loan_info.get("Customer Name", "")
                place = loan_info.get("Place", "")
            else:
                # Legacy Schema
                person_doc_id = path_parts[5]
                person_entry = persons_map.get(person_doc_id, {})
                person_name = person_entry.get("Name", "")
                place = person_entry.get("Place", "")
                
            transactions_list.append({
                "File ID": file_id,
                "File Name": file_name,
                "Customer Name": person_name,
                "Place": place,
                "Type": "RECEIVED (Payment)",
                "Amount (Rs)": pay_amount,
                "Date": pay_date_str,
                "Transaction ID": pay_id,
                "Is Deleted": pay_deleted
            })
except Exception as e:
    print(f"Error fetching payments/transactions: {e}")

# 5. Handle legacy schema GIVEN transactions
for person_doc_id, entry in persons_map.items():
    if person_doc_id not in persons_with_new_loans:
        person_data = entry["_raw_data"]
        amount = person_data.get("amountGiven") or person_data.get("totalAmount") or person_data.get("amount") or 0.0
        date_given = person_data.get("dateGiven") or person_data.get("createdAt")
        
        l_date_str = safe_datetime_str(date_given)
        
        if amount > 0:
            transactions_list.append({
                "File ID": entry["File ID"],
                "File Name": entry["File Name"],
                "Customer Name": entry["Name"],
                "Place": entry["Place"],
                "Type": "GIVEN (Loan)",
                "Amount (Rs)": amount,
                "Date": l_date_str,
                "Transaction ID": f"{entry['Customer ID']}_loan",
                "Is Deleted": entry["Is Deleted"]
            })

# 6. Format Customers list (remove internal keys)
for entry in persons_list:
    entry.pop("_raw_doc_id", None)
    entry.pop("_raw_data", None)
    customers_list.append({
        "File Name": entry["File Name"],
        "Customer ID": entry["Customer ID"],
        "Name": entry["Name"],
        "Mobile": entry["Mobile"],
        "Place": entry["Place"],
        "Notes": entry["Notes"],
        "Created At": entry["Created At"],
        "Is Deleted": entry["Is Deleted"]
    })

# Format Transactions list (remove File ID)
formatted_transactions = []
for entry in transactions_list:
    formatted_transactions.append({
        "File Name": entry["File Name"],
        "Customer Name": entry["Customer Name"],
        "Place": entry["Place"],
        "Type": entry["Type"],
        "Amount (Rs)": entry["Amount (Rs)"],
        "Date": entry["Date"],
        "Transaction ID": entry["Transaction ID"],
        "Is Deleted": entry["Is Deleted"]
    })

print("Formatting data and writing Excel sheets...")
print(f"Exported summary: {len(files_list)} files, {len(customers_list)} customers, {len(formatted_transactions)} transactions")

if not files_list or not customers_list:
    import sys
    print("\nERROR: No data could be fetched from Firestore (possibly due to quota limit). Aborting save to protect existing backup files.")
    sys.exit(1)

# Convert lists to DataFrames
df_files = pd.DataFrame(files_list) if files_list else pd.DataFrame(columns=["File ID", "File Name", "Created At", "Is Deleted"])
df_customers = pd.DataFrame(customers_list) if customers_list else pd.DataFrame(columns=["File Name", "Customer ID", "Name", "Mobile", "Place", "Notes", "Created At", "Is Deleted"])
df_transactions = pd.DataFrame(formatted_transactions) if formatted_transactions else pd.DataFrame(columns=["File Name", "Customer Name", "Place", "Type", "Amount (Rs)", "Date", "Transaction ID", "Is Deleted"])

excel_path = "moneymate_backup.xlsx"

# Write DataFrames to Excel sheets
try:
    with pd.ExcelWriter(excel_path, engine="openpyxl") as writer:
        df_files.to_excel(writer, sheet_name="Files", index=False)
        df_customers.to_excel(writer, sheet_name="Customers", index=False)
        df_transactions.to_excel(writer, sheet_name="Transactions", index=False)
    print(f"\nSUCCESS: Combined Excel backup created successfully at '{os.path.abspath(excel_path)}'!")
except Exception as e:
    print(f"\nERROR: Failed to write {excel_path}: {e}")
    print("Please close the file if it is open in Excel and run again.")

# Write individual files
from collections import Counter
file_names = [f["File Name"] for f in files_list]
name_counts = Counter(file_names)

print("\nWriting individual file backups...")
for file_id, file_name in files_map.items():
    f_cust = [c for c in customers_list if c["File Name"] == file_name]
    f_trans = [t for t in formatted_transactions if t["File Name"] == file_name]
    
    if name_counts[file_name] > 1:
        safe_id = str(file_id)[:8]
        file_excel_path = f"{file_name}_{safe_id}.xlsx"
    else:
        file_excel_path = f"{file_name}.xlsx"
        
    df_f_cust = pd.DataFrame(f_cust) if f_cust else pd.DataFrame(columns=["Customer ID", "Name", "Mobile", "Place", "Notes", "Created At", "Is Deleted"])
    if not df_f_cust.empty and "File Name" in df_f_cust.columns:
        df_f_cust = df_f_cust.drop(columns=["File Name"])
        
    df_f_trans = pd.DataFrame(f_trans) if f_trans else pd.DataFrame(columns=["Customer Name", "Place", "Type", "Amount (Rs)", "Date", "Transaction ID", "Is Deleted"])
    if not df_f_trans.empty and "File Name" in df_f_trans.columns:
        df_f_trans = df_f_trans.drop(columns=["File Name"])
        
    try:
        with pd.ExcelWriter(file_excel_path, engine="openpyxl") as writer:
            df_f_cust.to_excel(writer, sheet_name="Customers", index=False)
            df_f_trans.to_excel(writer, sheet_name="Transactions", index=False)
        print(f"  Created separate backup: '{os.path.abspath(file_excel_path)}'")
    except Exception as e:
        print(f"  ERROR: Failed to write {file_excel_path}: {e}")
        print("  Please close the file if it is open in Excel.")
