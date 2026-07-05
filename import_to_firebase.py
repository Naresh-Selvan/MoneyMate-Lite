import os
import sys
import time
import random
import argparse
import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore
from firebase_admin import auth

def parse_names_file(file_path):
    books = {
        1: {"name": "NLR 1", "customers": []},
        2: {"name": "NLR 2", "customers": []},
        3: {"name": "NLR 3", "customers": []},
        4: {"name": "NLR 4", "customers": []}
    }
    
    current_book = None
    
    if not os.path.exists(file_path):
        print(f"Error: {file_path} not found!")
        sys.exit(1)
        
    with open(file_path, "r", encoding="utf-8") as f:
        for line in f:
            line_str = line.strip()
            if not line_str:
                continue
                
            # Detect book section
            if line_str.upper() == "NLR 1":
                current_book = 1
                continue
            elif line_str.upper() == "NLR 2":
                current_book = 2
                continue
            elif line_str.upper() == "NLR 3":
                current_book = 3
                continue
            elif line_str.upper() == "NLR 4":
                current_book = 4
                continue
                
            if current_book is None:
                continue
                
            # Parse customer name and notes
            name = line_str
            notes = None
            
            # Check for '='
            if "=" in name:
                parts = name.split("=", 1)
                name = parts[0].strip()
                val = parts[1].strip()
                if val:
                    notes = f"Balance: {val}"
                    
            # Strip trailing hyphens and whitespace
            while name.endswith("-"):
                name = name[:-1].strip()
                
            if not name:
                continue
                
            books[current_book]["customers"].append({
                "name": name,
                "notes": notes
            })
            
    return books

def main():
    parser = argparse.ArgumentParser(description="Import namelists into Firebase Firestore.")
    parser.add_argument("--uid", type=str, help="Firebase User ID (UID) of the account.")
    args = parser.parse_args()
    
    key_path = "firebase-key.json"
    if not os.path.exists(key_path):
        print("\nERROR: firebase-key.json is missing in the current directory!")
        print("Please place it here and run again.")
        sys.exit(1)
        
    print("Initializing Firebase...")
    cred = credentials.Certificate(key_path)
    firebase_admin.initialize_app(cred)
    db = firestore.client()
    
    target_email = "selvadhana2007@gmail.com"
    default_password = "nareshs@06052023"
    uid = args.uid
    
    if not uid:
        print(f"\nChecking for target user: {target_email}...")
        try:
            user = auth.get_user_by_email(target_email)
            uid = user.uid
            print(f"User already exists! UID: {uid}")
        except auth.UserNotFoundError:
            print(f"User {target_email} does not exist in Firebase. Creating it now...")
            try:
                user = auth.create_user(
                    email=target_email,
                    email_verified=True,
                    password=default_password,
                    display_name="Dhana Selvan"
                )
                uid = user.uid
                print(f"User created successfully!")
                print(f"  Email: {target_email}")
                print(f"  Password set as: {default_password}")
                print(f"  UID: {uid}")
            except Exception as create_err:
                print(f"Failed to create user: {create_err}")
                sys.exit(1)
        except Exception as e:
            print(f"Error checking user: {e}")
            uid = input("\nPlease paste the Firebase User ID (UID) manually: ").strip()
            if not uid:
                print("User ID is required.")
                sys.exit(1)
                
    print(f"\nUsing Firebase User ID (UID): {uid}")
    
    # Parse names_input.txt
    books = parse_names_file("names_input.txt")
    
    # Display parsing summary
    print("\nParsed books and customer counts:")
    for book_id, info in books.items():
        print(f"  {info['name']}: {len(info['customers'])} customers")
        
    # Since running from non-interactive terminal sometimes, let's auto-confirm or accept input
    print("\nStarting upload to Firestore...")
    
    batch = db.batch()
    batch_count = 0
    total_customers = 0
    
    for book_id, info in books.items():
        file_name = info["name"]
        customers = info["customers"]
        
        # 1. Create or set the Loan File document
        # path: files/{uid}/loan_files/{book_id}
        file_ref = db.collection("files").document(uid).collection("loan_files").document(str(book_id))
        file_data = {
            "id": book_id,
            "name": file_name,
            "createdAt": int(time.time() * 1000),
            "isDeleted": False
        }
        batch.set(file_ref, file_data)
        batch_count += 1
        
        # 2. Add customers
        # path: files/{uid}/loan_files/{book_id}/persons/{person_id}
        for index, cust in enumerate(customers):
            # Generate a unique positive 63-bit integer for person ID
            person_id = random.randint(1, 2**63 - 1)
            person_ref = file_ref.collection("persons").document(str(person_id))
            
            person_data = {
                "id": person_id,
                "fileId": str(book_id),
                "name": cust["name"],
                "mobileNumber": "",
                "place": "",
                "notes": cust["notes"] or "",
                "sortOrder": index,
                "createdAt": int(time.time() * 1000),
                "isDeleted": False
            }
            
            batch.set(person_ref, person_data)
            batch_count += 1
            total_customers += 1
            
            # Firestore batch size limit is 500 operations
            if batch_count >= 400:
                print(f"Committing batch of {batch_count} operations...")
                batch.commit()
                batch = db.batch()
                batch_count = 0
                
    if batch_count > 0:
        print(f"Committing final batch of {batch_count} operations...")
        batch.commit()
        
    print(f"\nSUCCESS! Uploaded 4 books and {total_customers} customers successfully into the new database for {target_email}.")

if __name__ == "__main__":
    main()
