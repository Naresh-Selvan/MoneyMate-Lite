import pandas as pd
import os

# Read the combined Excel file
excel_path = "moneymate_backup.xlsx"
if not os.path.exists(excel_path):
    print(f"Error: {excel_path} not found!")
    exit(1)

print("Reading combined backup file...")
df_files = pd.read_excel(excel_path, sheet_name="Files")
df_customers = pd.read_excel(excel_path, sheet_name="Customers")
df_transactions = pd.read_excel(excel_path, sheet_name="Transactions")

file_mappings = [
    {"id": "1", "name": "NLR 1", "output": "NLR 1_1.xlsx", "filter_name": "NLR 1_dummy"},
    {"id": "6e123a58-7f20-442f-b48a-794d756062f3", "name": "NLR 3", "output": "NLR 3.xlsx", "filter_name": "NLR 3"},
    {"id": "c07683af-00ea-48c6-8806-fdcde1c1b062", "name": "NLR 4", "output": "NLR 4.xlsx", "filter_name": "NLR 4"},
    {"id": "f28b4b4b-66a6-472a-8ee2-ff0e01f6d975", "name": "NLR 1", "output": "NLR 1_f28b4b4b.xlsx", "filter_name": "NLR 1"},
    {"id": "f6d41a27-7f70-49ae-b80e-5fa104127f89", "name": "NLR 2", "output": "NLR 2.xlsx", "filter_name": "NLR 2"}
]

print("Splitting backups...")
for m in file_mappings:
    output_path = m["output"]
    filter_name = m["filter_name"]
    
    # Filter customers and transactions
    cust_filtered = df_customers[df_customers["File Name"] == filter_name].copy()
    trans_filtered = df_transactions[df_transactions["File Name"] == filter_name].copy()
    
    # Drop "File Name" column for individual sheets
    if "File Name" in cust_filtered.columns:
        cust_filtered = cust_filtered.drop(columns=["File Name"])
    if "File Name" in trans_filtered.columns:
        trans_filtered = trans_filtered.drop(columns=["File Name"])
        
    try:
        with pd.ExcelWriter(output_path, engine="openpyxl") as writer:
            cust_filtered.to_excel(writer, sheet_name="Customers", index=False)
            trans_filtered.to_excel(writer, sheet_name="Transactions", index=False)
        print(f"  Created separate backup: '{os.path.abspath(output_path)}' (customers: {len(cust_filtered)}, transactions: {len(trans_filtered)})")
    except Exception as e:
        print(f"  Error writing separate backup {output_path}: {e}")

print("Done split backups!")
