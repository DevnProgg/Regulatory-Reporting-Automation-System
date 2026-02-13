import psycopg2
import random
from faker import Faker
from datetime import datetime, timedelta
import os
from dotenv import load_dotenv

#load environment variables from .env file
load_dotenv()

# CONFIGURATION
DB_CONFIG = {
    "dbname": os.getenv("DBNAME"),
    "username": os.getenv("USERNAME"),
    "password": os.getenv("PASSWORD"),
    "host": "localhost",
    "port": os.getenv("PORT", 5432)
}


print(DB_CONFIG)

# Initialize Faker
fake = Faker()

# LESOTHO SPECIFIC DATA SETS
# Custom lists to ensure data looks authentic for Lesotho
TOWNS = [
    "Maseru", "Teyateyaneng", "Mafeteng", "Hlotse", "Mohale's Hoek",
    "Maputsoe", "Qacha's Nek", "Quthing", "Butha-Buthe", "Mokhotlong", "Thaba-Tseka"
]

FIRST_NAMES = [
    "Thabo", "Mpho", "Lerato", "Tsepo", "Moshoeshoe", "Nthabiseng",
    "Refiloe", "Khotso", "Palesa", "Rethabile", "Bokang", "Neo",
    "Limpho", "Teboho", "Karabo", "Maseeiso", "Lineo"
]

SURNAMES = [
    "Mokoena", "Dlamini", "Molapo", "Ramabanta", "Phiri", "Radebe",
    "Mosoeu", "Maqelepo", "Letsie", "Chabeli", "Nhlapo", "Sehloho",
    "Tau", "Moloi", "Tlali", "Majara"
]

CORPORATES = [
    "Maluti Mountain Brewery", "Letseng Diamonds", "Lesotho Flour Mills",
    "Matekane Group", "Lesotho Electricity Company", "Water and Sewerage Company",
    "Econet Telecom Lesotho", "Vodacom Lesotho", "Alliance Insurance",
    "Lesotho Post Bank", "Loti Brick", "Basotho Canners"
]


# HELPER FUNCTIONS

def get_db_connection():
    return psycopg2.connect(**DB_CONFIG)


def generate_lesotho_name():
    return f"{random.choice(FIRST_NAMES)} {random.choice(SURNAMES)}"


def determine_asset_class(dpd):
    """Maps Days Past Due to Lesotho CBL Asset Classification"""
    if dpd <= 30:
        return 'STANDARD'
    elif dpd <= 60:
        return 'WATCH'
    elif dpd <= 90:
        return 'SUBSTANDARD'
    elif dpd <= 180:
        return 'DOUBTFUL'
    else:
        return 'LOSS'


def determine_ifrs9_stage(asset_class):
    """Maps Asset Class to IFRS 9 Stage"""
    if asset_class == 'STANDARD':
        return 1
    elif asset_class == 'WATCH':
        return 2
    else:
        return 3


# DATA GENERATION FUNCTIONS

def insert_customers(connection, count=50):
    print(f"--- Generating {count} Customers ---")
    cursor = connection.cursor()
    customer_ids = []

    for _ in range(count):
        # 80% Retail, 20% Corporate/SME
        is_corporate = random.random() < 0.2

        if is_corporate:
            name = random.choice(CORPORATES) + " " + random.choice(["Holdings", "Ltd", "Pty Ltd", "Trading"])
            cust_type = random.choice(['CORP', 'SME'])
            is_financial = random.choice([True, False]) if cust_type == 'CORP' else False
        else:
            name = generate_lesotho_name()  # Only used for logging/logic, schema doesn't have name col (assumed PII separation)
            cust_type = 'RETAIL'
            is_financial = False

        country = "Lesotho"
        # 10% chance of foreign entity (e.g., South Africa)
        if random.random() < 0.1:
            country = "South Africa"

        sql = """
            INSERT INTO cbs.customers (
                customer_type, country, country_risk_rating, internal_rating, 
                external_rating, pd_value, lgd_value, is_financial_inst, is_public_sector
            ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
            RETURNING customer_id
        """

        # Ratings logic
        internal_rating = random.choice(['AAA', 'AA', 'A', 'BBB', 'BB', 'B', 'CCC'])
        pd_value = random.uniform(0.001, 0.15)  # 0.1% to 15% probability of default
        lgd_value = random.uniform(0.20, 0.60)  # 20% to 60% loss given default

        cursor.execute(sql, (
            cust_type, country, random.randint(2, 5), internal_rating,
            None, pd_value, lgd_value, is_financial, False
        ))
        customer_ids.append(cursor.fetchone()[0])

    connection.commit()
    print("Customers inserted.")
    return customer_ids


def insert_accounts(conn, customer_ids):
    print(f"--- Generating Accounts for {len(customer_ids)} Customers ---")
    cursor = conn.cursor()
    account_ids = []

    for cust_id in customer_ids:
        # Every customer gets 1-3 accounts
        num_accounts = random.randint(1, 3)

        for _ in range(num_accounts):
            acc_type = random.choice(['SAVINGS', 'CURRENT', 'LOAN'])
            currency = 'LSL'  # Lesotho Loti

            # Small chance of ZAR account
            if random.random() < 0.15:
                currency = 'ZAR'

            balance = round(random.uniform(500, 500000), 2)

            # If it's a loan account, balance is 0 initially (disbursed in loans table)
            if acc_type == 'LOAN':
                balance = 0

            sql = """
                INSERT INTO cbs.accounts (
                    customer_id, account_type, currency, balance, available_balance, status
                ) VALUES (%s, %s, %s, %s, %s, %s)
                RETURNING account_id, account_type
            """

            cursor.execute(sql, (cust_id, acc_type, currency, balance, balance, 'ACTIVE'))
            row = cursor.fetchone()

            # Store ID if it's a LOAN account for the next step
            if row[1] == 'LOAN':
                account_ids.append(row[0])

    conn.commit()
    print("Accounts inserted.")
    return account_ids


def insert_loans_and_performance(conn, loan_account_ids):
    print(f"--- Generating Loans and Performance for {len(loan_account_ids)} Accounts ---")
    cursor = conn.cursor()

    for acc_id in loan_account_ids:
        # Loan Details
        principal = round(random.uniform(10000, 5000000), 2)
        outstanding = principal * float.__float__(random.uniform(0.1, 1.0))  # Outstanding is 10-100% of principal
        rate = round(random.uniform(0.09, 0.28), 4)  # 9% to 28% interest

        origination = fake.date_between(start_date='-5y', end_date='-1y')
        term_months = random.choice([12, 24, 36, 60, 120, 240])  # Months
        maturity = origination + timedelta(days=term_months * 30)

        # Collateral logic
        prod_type = random.choice(['MORTGAGE', 'AUTO', 'PERSONAL', 'SME_LOAN'])
        collateral_type = 'NONE'
        collateral_val = 0

        if prod_type == 'MORTGAGE':
            collateral_type = 'PROPERTY'
            collateral_val = principal * float.__float__(1.2)  # 120% coverage
            purpose = 'RESIDENTIAL'
        elif prod_type == 'AUTO':
            collateral_type = 'VEHICLE'
            collateral_val = principal
            purpose = 'VEHICLE_PURCHASE'
        else:
            purpose = 'GENERAL_CONSUMPTION'

        # Performance / Delinquency Simulation
        # 85% chance of being a good payer (0 DPD)
        is_delinquent = random.random() > 0.85
        days_past_due = 0

        if is_delinquent:
            days_past_due = random.randint(1, 200)

        asset_class = determine_asset_class(days_past_due)
        stage = determine_ifrs9_stage(asset_class)

        # Insert Loan
        sql_loan = """
            INSERT INTO cbs.loans (
                account_id, principal_amount, outstanding_balance, interest_rate,
                origination_date, maturity_date, collateral_value, collateral_type,
                product_type, loan_purpose, asset_class, stage, 
                original_term_months, remaining_term_months
            ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            RETURNING loan_id
        """

        cursor.execute(sql_loan, (
            acc_id, principal, outstanding, rate, origination, maturity,
            collateral_val, collateral_type, prod_type, purpose,
            asset_class, stage, term_months, max(0, term_months - 12)
        ))
        loan_id = cursor.fetchone()[0]

        # Insert Performance
        sql_perf = """
            INSERT INTO cbs.loan_performance (
                loan_id, days_past_due, last_payment_date, last_payment_amount
            ) VALUES (%s, %s, %s, %s)
        """
        last_pay = datetime.now() - timedelta(days=days_past_due) if days_past_due > 0 else datetime.now()
        cursor.execute(sql_perf, (loan_id, days_past_due, last_pay, principal / term_months))

    conn.commit()
    print("Loans and Performance data inserted.")


def insert_capital_components(conn):
    print("--- Generating Basel III Capital Components ---")
    cursor = conn.cursor()

    # Realistic Capital stack for a mid-sized Lesotho bank
    # Amounts in LSL
    capital_data = [
        ('CET1', 'Paid Up Ordinary Shares', 500000000, 0),
        ('CET1', 'Retained Earnings', 250000000, 0),
        ('CET1', 'Statutory Reserves', 50000000, 0),
        ('AT1', 'Perpetual Non-Cumulative Pref Shares', 100000000, 0),
        ('T2', 'Subordinated Debt', 150000000, 0),
        ('T2', 'General Provisions (Standard Assets)', 25000000, 0),
    ]

    sql = """
        INSERT INTO cbs.capital_components (
            as_of_date, component_type, component_name, amount, currency, regulatory_adjustment
        ) VALUES (%s, %s, %s, %s, %s, %s)
        ON CONFLICT DO NOTHING
    """

    today = datetime.now().date()

    for row in capital_data:
        cursor.execute(sql, (today, row[0], row[1], row[2], 'LSL', row[3]))

    conn.commit()
    print("Capital Components inserted.")


def insert_liquidity_assets(conn):
    print("--- Generating LCR Liquidity Assets ---")
    cursor = conn.cursor()

    # HQLA Assets
    assets = [
        ('CENTRAL_BANK_RESERVES', 'LSL', 120000000, 0.00, 1),  # Level 1, 0% haircut
        ('GOVT_SECURITIES', 'LSL', 300000000, 0.00, 1),  # Lesotho Govt Bonds, Level 1
        ('GOVT_SECURITIES', 'ZAR', 150000000, 0.00, 1),  # SA Govt Bonds, Level 1
        ('CASH', 'LSL', 45000000, 0.00, 1),  # Physical Cash
        ('CORP_BONDS', 'ZAR', 50000000, 0.15, 2),  # Level 2A, 15% haircut
    ]

    sql = """
        INSERT INTO cbs.liquidity_assets (
            asset_type, currency, market_value, haircut_percentage, 
            hqla_level, is_unencumbered, as_of_date
        ) VALUES (%s, %s, %s, %s, %s, %s, %s)
    """

    today = datetime.now().date()

    for row in assets:
        cursor.execute(sql, (row[0], row[1], row[2], row[3], row[4], True, today))

    conn.commit()
    print("Liquidity Assets inserted.")


# MAIN EXECUTION

def main():
    global conn
    try:
        conn = get_db_connection()
        print("Connected to Database.")

        # 1. Create Customers
        cust_ids = insert_customers(conn, count=100)

        # 2. Create Accounts
        acc_ids = insert_accounts(conn, cust_ids)

        # 3. Create Loans & Performance (Only for accounts typed as LOAN)
        insert_loans_and_performance(conn, acc_ids)

        # 4. Create Regulatory Data
        insert_capital_components(conn)
        insert_liquidity_assets(conn)

        print("\nSUCCESS: Dummy data generation complete.")

    except Exception as e:
        print(f"ERROR: {e}")
    finally:
        if conn:
            conn.close()


if __name__ == "__main__":
    main()