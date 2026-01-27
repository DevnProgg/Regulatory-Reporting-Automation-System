import uuid
import random
import requests
import time
from datetime import datetime, timedelta
from decimal import Decimal
import json
from typing import Dict, List, Set
import logging

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class LesothoBankSimulator:
    """Simulates real-time banking transactions for Lesotho financial institutions"""

    def __init__(self, api_endpoint: str, api_key: str = None):
        self.api_endpoint = api_endpoint
        self.api_key = api_key
        self.session = requests.Session()
        if api_key:
            self.session.headers.update({'Authorization': f'Bearer {api_key}'})

        # Track unique identifiers to prevent duplication
        self.used_transaction_ids: Set[str] = set()
        self.used_loan_ids: Set[str] = set()
        self.used_account_ids: Set[str] = set()

        # Lesotho-specific data
        self.currency = 'LSL'  # Lesotho Loti
        self.country_code = 'LS'

        self.lesotho_names = {
            'male_first': ['Thabo', 'Tshepo', 'Lehlohonolo', 'Mohlomi', 'Retšelisitsoe',
                           'Tšepo', 'Kabelo', 'Molefe', 'Tumelo', 'Lebohang'],
            'female_first': ['Palesa', 'Mamello', 'Lineo', 'Rethabile', 'Mpho',
                             'Keitumetse', 'Mathabo', 'Refiloe', 'Tšepiso', 'Naledi'],
            'surnames': ['Molapo', 'Moshoeshoe', 'Ramosoeu', 'Letsie', 'Mohale',
                         'Mofolo', 'Ntšo', 'Khethisa', 'Moloi', 'Sekhonyana',
                         'Tau', 'Mokone', 'Tlali', 'Mphoso', 'Ntsane']
        }

        self.business_names = [
            'Maseru Wholesalers', 'Leribe Trading Co', 'Mafeteng Textiles',
            'Roma Valley Farms', 'Teyateyaneng Crafts', 'Mokhotlong Mining',
            'Butha-Buthe Transport', 'Quthing Stores', "Mohale's Hoek Traders",
            'Thaba-Tseka Enterprises', 'Basotho Hat Manufacturing',
            'Lesotho Wool Exporters', 'Maluti Mountain Lodge', 'Katse Dam Services'
        ]

        self.sectors = [
            'AGRICULTURE', 'MINING', 'MANUFACTURING', 'RETAIL', 'WHOLESALE',
            'TRANSPORT', 'HOSPITALITY', 'CONSTRUCTION', 'TEXTILE', 'FINANCIAL'
        ]

        self.collateral_types = [
            'REAL_ESTATE', 'VEHICLE', 'LIVESTOCK', 'EQUIPMENT',
            'INVENTORY', 'LAND', 'WOOL_STOCK', 'MOHAIR_STOCK'
        ]

        # Initialize bank
        self.bank_id = str(uuid.uuid4())
        self.initialize_bank()

    def initialize_bank(self):
        """Initialize a sample bank"""
        self.bank_data = {
            'bank_id': self.bank_id,
            'bank_code': 'LSBL001',
            'bank_name': 'Lesotho Standard Bank',
            'license_number': 'CBL/2024/001',
            'reporting_currency': self.currency,
            'created_at': datetime.now().isoformat()
        }
        logger.info(f"Initialized bank: {self.bank_data['bank_name']}")

    def generate_unique_uuid(self, used_set: Set[str]) -> str:
        """Generate a UUID that hasn't been used before"""
        while True:
            new_id = str(uuid.uuid4())
            if new_id not in used_set:
                used_set.add(new_id)
                return new_id

    def generate_customer(self, customer_type: str = None) -> Dict:
        """Generate a unique customer"""
        if customer_type is None:
            customer_type = random.choice(['INDIVIDUAL', 'CORPORATE'])

        customer = {
            'customer_id': str(uuid.uuid4()),
            'customer_type': customer_type,
            'sector_code': random.choice(self.sectors),
            'country_code': self.country_code,
            'created_at': datetime.now().isoformat()
        }

        if customer_type == 'INDIVIDUAL':
            gender = random.choice(['male', 'female'])
            first_name = random.choice(self.lesotho_names[f'{gender}_first'])
            surname = random.choice(self.lesotho_names['surnames'])
            customer['name'] = f"{first_name} {surname}"
        else:
            customer['name'] = random.choice(self.business_names)

        return customer

    def generate_account(self, customer_id: str, account_type: str = None) -> Dict:
        """Generate a unique account"""
        if account_type is None:
            account_type = random.choice(['LOAN', 'DEPOSIT', 'NOSTRO'])

        account_id = self.generate_unique_uuid(self.used_account_ids)
        opened_at = datetime.now() - timedelta(days=random.randint(30, 1095))

        account = {
            'account_id': account_id,
            'bank_id': self.bank_id,
            'customer_id': customer_id,
            'account_type': account_type,
            'currency': self.currency,
            'opened_at': opened_at.date().isoformat(),
            'status': 'ACTIVE'
        }

        if account_type == 'LOAN':
            account['maturity_date'] = (opened_at + timedelta(days=random.randint(365, 3650))).date().isoformat()

        return account

    def generate_loan_exposure(self, account_id: str) -> Dict:
        """Generate a unique loan exposure transaction"""
        loan_id = self.generate_unique_uuid(self.used_loan_ids)

        # Realistic loan amounts for Lesotho (LSL)
        outstanding_balance = round(random.uniform(10000, 5000000), 2)
        accrued_interest = round(outstanding_balance * random.uniform(0.01, 0.15), 2)

        # Calculate days past due with weighted distribution
        dpd_weights = [0.7, 0.15, 0.08, 0.05, 0.02]  # Most loans current
        dpd_ranges = [0, random.randint(1, 30), random.randint(31, 60),
                      random.randint(61, 90), random.randint(91, 180)]
        days_past_due = random.choices(dpd_ranges, weights=dpd_weights)[0]

        # Collateral
        collateral_type = random.choice(self.collateral_types)
        collateral_value = round(outstanding_balance * random.uniform(0.8, 2.0), 2)

        # Risk parameters
        pd = round(random.uniform(0.01, 0.25), 4)  # Probability of default
        lgd = round(random.uniform(0.25, 0.75), 4)  # Loss given default
        ead = round(outstanding_balance + accrued_interest, 2)  # Exposure at default

        # Risk weight based on Basel standards
        if days_past_due > 90:
            risk_weight = 1.5
        elif pd > 0.15:
            risk_weight = 1.0
        else:
            risk_weight = round(random.uniform(0.35, 1.0), 4)

        npl_status = days_past_due > 90

        return {
            'loan_id': loan_id,
            'account_id': account_id,
            'outstanding_balance': outstanding_balance,
            'accrued_interest': accrued_interest,
            'days_past_due': days_past_due,
            'collateral_value': collateral_value,
            'collateral_type': collateral_type,
            'probability_of_default': pd,
            'loss_given_default': lgd,
            'exposure_at_default': ead,
            'risk_weight': risk_weight,
            'npl_status': npl_status,
            'snapshot_date': datetime.now().date().isoformat()
        }

    def generate_off_balance_sheet_exposure(self) -> Dict:
        """Generate off-balance sheet exposure"""
        exposure_types = ['GUARANTEE', 'LETTER_OF_CREDIT', 'COMMITMENT']
        ccf_map = {  # Credit conversion factors
            'GUARANTEE': 1.0,
            'LETTER_OF_CREDIT': 0.5,
            'COMMITMENT': 0.2
        }

        exposure_type = random.choice(exposure_types)
        notional_amount = round(random.uniform(50000, 2000000), 2)

        return {
            'obs_id': str(uuid.uuid4()),
            'bank_id': self.bank_id,
            'exposure_type': exposure_type,
            'notional_amount': notional_amount,
            'credit_conversion_factor': ccf_map[exposure_type],
            'counterparty_sector': random.choice(self.sectors),
            'snapshot_date': datetime.now().date().isoformat()
        }

    def generate_capital_component(self) -> Dict:
        """Generate capital component data"""
        capital_types = ['CET1', 'AT1', 'T2']
        capital_type = random.choice(capital_types)

        # Realistic capital amounts
        amount_ranges = {
            'CET1': (5000000, 50000000),
            'AT1': (1000000, 10000000),
            'T2': (500000, 5000000)
        }

        amount = round(random.uniform(*amount_ranges[capital_type]), 2)
        eligible = random.random() > 0.1  # 90% eligible

        return {
            'capital_id': str(uuid.uuid4()),
            'bank_id': self.bank_id,
            'capital_type': capital_type,
            'amount': amount,
            'eligible': eligible,
            'snapshot_date': datetime.now().date().isoformat()
        }

    def generate_liquidity_cashflow(self) -> Dict:
        """Generate liquidity cashflow data"""
        flow_type = random.choice(['INFLOW', 'OUTFLOW'])
        maturity_buckets = ['0-7d', '8-30d', '31-90d', '91-180d', '181-365d']

        amount = round(random.uniform(100000, 10000000), 2)
        stress_factor = round(random.uniform(0.5, 1.0), 4)

        return {
            'cashflow_id': str(uuid.uuid4()),
            'bank_id': self.bank_id,
            'flow_type': flow_type,
            'amount': amount,
            'currency': self.currency,
            'maturity_bucket': random.choice(maturity_buckets),
            'stress_factor': stress_factor,
            'snapshot_date': datetime.now().date().isoformat()
        }

    def send_to_api(self, endpoint: str, data: Dict) -> bool:
        """Send data to API endpoint"""
        try:
            url = f"{self.api_endpoint}/{endpoint}"
            response = self.session.post(url, json=data, timeout=10)

            if response.status_code in [200, 201]:
                logger.info(f"✓ Sent {endpoint}: {data.get('loan_id', data.get('obs_id', 'N/A'))[:8]}...")
                return True
            else:
                logger.error(f"✗ Failed {endpoint}: {response.status_code} - {response.text}")
                return False
        except requests.exceptions.RequestException as e:
            logger.error(f"✗ API Error on {endpoint}: {str(e)}")
            return False

    def run_simulation(self, duration_hours: int = 2, transactions_per_minute: int = 5):
        """
        Run the simulation for specified duration

        Args:
            duration_hours: How long to run the simulation
            transactions_per_minute: Rate of transaction generation
        """
        logger.info(f"Starting simulation for {duration_hours} hours at {transactions_per_minute} tx/min")

        end_time = datetime.now() + timedelta(hours=duration_hours)
        transaction_count = 0

        while datetime.now() < end_time:
            try:
                # Generate and send transactions
                for _ in range(transactions_per_minute):
                    # Generate complete transaction flow
                    customer = self.generate_customer()
                    account = self.generate_account(customer['customer_id'], 'LOAN')
                    loan = self.generate_loan_exposure(account['account_id'])

                    # Send to API (modify endpoints as needed)
                    self.send_to_api('customers', customer)
                    self.send_to_api('accounts', account)
                    self.send_to_api('loan_exposures', loan)

                    # Occasionally generate other transaction types
                    if random.random() < 0.3:
                        obs = self.generate_off_balance_sheet_exposure()
                        self.send_to_api('off_balance_sheet', obs)

                    if random.random() < 0.1:
                        capital = self.generate_capital_component()
                        self.send_to_api('capital_components', capital)

                    if random.random() < 0.2:
                        cashflow = self.generate_liquidity_cashflow()
                        self.send_to_api('liquidity_cashflows', cashflow)

                    transaction_count += 1

                # Wait for next minute
                logger.info(f"Total transactions: {transaction_count} | Unique loans: {len(self.used_loan_ids)}")
                time.sleep(60)

            except KeyboardInterrupt:
                logger.info("Simulation stopped by user")
                break
            except Exception as e:
                logger.error(f"Error in simulation loop: {str(e)}")
                time.sleep(5)

        logger.info(f"Simulation completed. Total transactions: {transaction_count}")
        self.print_statistics()

    def print_statistics(self):
        """Print simulation statistics"""
        print("\n" + "=" * 50)
        print("SIMULATION STATISTICS")
        print("=" * 50)
        print(f"Unique Accounts Created: {len(self.used_account_ids)}")
        print(f"Unique Loans Created: {len(self.used_loan_ids)}")
        print(f"Bank: {self.bank_data['bank_name']}")
        print(f"Currency: {self.currency}")
        print("=" * 50 + "\n")


if __name__ == "__main__":
    # Configuration
    API_ENDPOINT = "http://localhost:8000/api/v1"  # Change to your API endpoint
    API_KEY = None  # Add your API key if required

    # Initialize simulator
    simulator = LesothoBankSimulator(
        api_endpoint=API_ENDPOINT,
        api_key=API_KEY
    )

    # Run simulation
    # For testing: 0.1 hours = 6 minutes
    # For production: 24 hours = full day
    simulator.run_simulation(
        duration_hours=2,
        transactions_per_minute=5
    )