# HisabPro - Supabase PostgreSQL Schema & Row-Level Security (RLS) Setup

This document provides the PostgreSQL DDL statements and Row-Level Security (RLS) policies required for HisabPro Multi-Tenant Cloud Synchronization.

> **CRITICAL WARNING**:
> **REVIEW BEFORE APPLYING** - Verify these SQL statements in a staging/development environment before applying them to your production Supabase database instance.

---

## 1. Table Definitions & Schema DDL

```sql
-- REVIEW BEFORE APPLYING

-- 1. Businesses
CREATE TABLE IF NOT EXISTS public.businesses (
    id TEXT PRIMARY KEY NOT NULL,
    user_id UUID NOT NULL DEFAULT auth.uid(),
    name TEXT NOT NULL,
    owner_name TEXT NOT NULL DEFAULT '',
    address TEXT NOT NULL DEFAULT '',
    phone TEXT NOT NULL DEFAULT '',
    email TEXT NOT NULL DEFAULT '',
    gstin TEXT NOT NULL DEFAULT '',
    pan TEXT NOT NULL DEFAULT '',
    logo_path TEXT NOT NULL DEFAULT '',
    gst_enabled BOOLEAN NOT NULL DEFAULT false,
    financial_year_start TEXT NOT NULL DEFAULT '01-04',
    upi_id TEXT NOT NULL DEFAULT '',
    bank_name TEXT NOT NULL DEFAULT '',
    account_number TEXT NOT NULL DEFAULT '',
    ifsc_code TEXT NOT NULL DEFAULT '',
    terms_and_conditions TEXT NOT NULL DEFAULT '',
    created_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    updated_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    deleted_at BIGINT,
    synced_at BIGINT
);

-- 2. Parties (Customers & Suppliers)
CREATE TABLE IF NOT EXISTS public.parties (
    id TEXT PRIMARY KEY NOT NULL,
    user_id UUID NOT NULL DEFAULT auth.uid(),
    business_id TEXT NOT NULL REFERENCES public.businesses(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    phone TEXT NOT NULL DEFAULT '',
    email TEXT NOT NULL DEFAULT '',
    address TEXT NOT NULL DEFAULT '',
    gstin TEXT NOT NULL DEFAULT '',
    type TEXT NOT NULL DEFAULT 'CUSTOMER',
    tag TEXT NOT NULL DEFAULT 'REGULAR',
    opening_balance BIGINT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    updated_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    deleted_at BIGINT,
    synced_at BIGINT
);

-- 3. Items (Products & Services)
CREATE TABLE IF NOT EXISTS public.items (
    id TEXT PRIMARY KEY NOT NULL,
    user_id UUID NOT NULL DEFAULT auth.uid(),
    business_id TEXT NOT NULL REFERENCES public.businesses(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    item_code TEXT NOT NULL DEFAULT '',
    unit TEXT NOT NULL DEFAULT 'Pcs',
    hsn_code TEXT NOT NULL DEFAULT '',
    purchase_price BIGINT NOT NULL DEFAULT 0,
    sell_price BIGINT NOT NULL DEFAULT 0,
    gst_rate DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    category TEXT NOT NULL DEFAULT 'General',
    stock_qty DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    low_stock_threshold DOUBLE PRECISION NOT NULL DEFAULT 5.0,
    created_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    updated_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    deleted_at BIGINT,
    synced_at BIGINT
);

-- 4. Invoices
CREATE TABLE IF NOT EXISTS public.invoices (
    id TEXT PRIMARY KEY NOT NULL,
    user_id UUID NOT NULL DEFAULT auth.uid(),
    business_id TEXT NOT NULL REFERENCES public.businesses(id) ON DELETE CASCADE,
    invoice_no TEXT NOT NULL,
    date BIGINT NOT NULL,
    party_id TEXT REFERENCES public.parties(id) ON DELETE SET NULL,
    customer_name TEXT NOT NULL DEFAULT '',
    customer_phone TEXT NOT NULL DEFAULT '',
    customer_address TEXT NOT NULL DEFAULT '',
    customer_gstin TEXT NOT NULL DEFAULT '',
    type TEXT NOT NULL DEFAULT 'NON_GST_BILL',
    gst_mode TEXT NOT NULL DEFAULT 'EXEMPT',
    subtotal BIGINT NOT NULL DEFAULT 0,
    discount BIGINT NOT NULL DEFAULT 0,
    taxable_amount BIGINT NOT NULL DEFAULT 0,
    cgst BIGINT NOT NULL DEFAULT 0,
    sgst BIGINT NOT NULL DEFAULT 0,
    igst BIGINT NOT NULL DEFAULT 0,
    total BIGINT NOT NULL DEFAULT 0,
    paid_amount BIGINT NOT NULL DEFAULT 0,
    payment_status TEXT NOT NULL DEFAULT 'PAID',
    payment_mode TEXT NOT NULL DEFAULT 'Cash',
    notes TEXT NOT NULL DEFAULT '',
    is_gst BOOLEAN NOT NULL DEFAULT false,
    created_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    updated_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    deleted_at BIGINT,
    synced_at BIGINT
);

-- 5. Invoice Line Items
CREATE TABLE IF NOT EXISTS public.invoice_items (
    id TEXT PRIMARY KEY NOT NULL,
    user_id UUID NOT NULL DEFAULT auth.uid(),
    invoice_id TEXT NOT NULL REFERENCES public.invoices(id) ON DELETE CASCADE,
    item_id TEXT REFERENCES public.items(id) ON DELETE SET NULL,
    item_name TEXT NOT NULL,
    hsn_code TEXT NOT NULL DEFAULT '',
    qty DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    unit TEXT NOT NULL DEFAULT 'Pcs',
    rate BIGINT NOT NULL DEFAULT 0,
    discount BIGINT NOT NULL DEFAULT 0,
    cgst_rate DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    sgst_rate DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    igst_rate DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    amount BIGINT NOT NULL DEFAULT 0,
    deleted_at BIGINT,
    synced_at BIGINT
);

-- 6. Payments
CREATE TABLE IF NOT EXISTS public.payments (
    id TEXT PRIMARY KEY NOT NULL,
    user_id UUID NOT NULL DEFAULT auth.uid(),
    business_id TEXT NOT NULL REFERENCES public.businesses(id) ON DELETE CASCADE,
    party_id TEXT REFERENCES public.parties(id) ON DELETE SET NULL,
    date BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    mode TEXT NOT NULL DEFAULT 'Cash',
    reference_no TEXT NOT NULL DEFAULT '',
    notes TEXT NOT NULL DEFAULT '',
    linked_invoice_id TEXT REFERENCES public.invoices(id) ON DELETE SET NULL,
    created_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    updated_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    deleted_at BIGINT,
    synced_at BIGINT
);

-- 7. Expenses
CREATE TABLE IF NOT EXISTS public.expenses (
    id TEXT PRIMARY KEY NOT NULL,
    user_id UUID NOT NULL DEFAULT auth.uid(),
    business_id TEXT NOT NULL REFERENCES public.businesses(id) ON DELETE CASCADE,
    date BIGINT NOT NULL,
    category TEXT NOT NULL,
    amount BIGINT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    mode TEXT NOT NULL DEFAULT 'Cash',
    receipt_path TEXT NOT NULL DEFAULT '',
    created_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    updated_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    deleted_at BIGINT,
    synced_at BIGINT
);

-- 8. Accounts
CREATE TABLE IF NOT EXISTS public.accounts (
    id TEXT PRIMARY KEY NOT NULL,
    user_id UUID NOT NULL DEFAULT auth.uid(),
    business_id TEXT NOT NULL REFERENCES public.businesses(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    opening_balance BIGINT NOT NULL DEFAULT 0,
    account_number TEXT NOT NULL DEFAULT '',
    ifsc_code TEXT NOT NULL DEFAULT '',
    created_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    updated_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    deleted_at BIGINT,
    synced_at BIGINT
);

-- 9. Journal Entries
CREATE TABLE IF NOT EXISTS public.journal_entries (
    id TEXT PRIMARY KEY NOT NULL,
    user_id UUID NOT NULL DEFAULT auth.uid(),
    business_id TEXT NOT NULL REFERENCES public.businesses(id) ON DELETE CASCADE,
    date BIGINT NOT NULL,
    voucher_no TEXT NOT NULL DEFAULT '',
    narration TEXT NOT NULL DEFAULT '',
    created_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    updated_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    deleted_at BIGINT,
    synced_at BIGINT
);

-- 10. Journal Entry Lines
CREATE TABLE IF NOT EXISTS public.journal_entry_lines (
    id TEXT PRIMARY KEY NOT NULL,
    user_id UUID NOT NULL DEFAULT auth.uid(),
    journal_entry_id TEXT NOT NULL REFERENCES public.journal_entries(id) ON DELETE CASCADE,
    account_id TEXT NOT NULL REFERENCES public.accounts(id) ON DELETE CASCADE,
    account_name TEXT NOT NULL DEFAULT '',
    is_debit BOOLEAN NOT NULL DEFAULT true,
    debit BIGINT NOT NULL DEFAULT 0,
    credit BIGINT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    updated_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    deleted_at BIGINT,
    synced_at BIGINT
);

-- 11. Khata Entries (Party Ledger)
CREATE TABLE IF NOT EXISTS public.khata_entries (
    id TEXT PRIMARY KEY NOT NULL,
    user_id UUID NOT NULL DEFAULT auth.uid(),
    business_id TEXT NOT NULL REFERENCES public.businesses(id) ON DELETE CASCADE,
    party_id TEXT NOT NULL REFERENCES public.parties(id) ON DELETE CASCADE,
    amount BIGINT NOT NULL DEFAULT 0,
    type TEXT NOT NULL,
    date BIGINT NOT NULL,
    bill_number TEXT NOT NULL DEFAULT '',
    note TEXT NOT NULL DEFAULT '',
    created_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    updated_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    deleted_at BIGINT,
    synced_at BIGINT
);
```

---

## 2. Row-Level Security (RLS) Policies

> **SECURITY PRINCIPLE**:
> A user can **only** SELECT, INSERT, UPDATE, or DELETE records where `user_id = auth.uid()`.
> For child tables, records must additionally belong to a business owned by `auth.uid()`.

```sql
-- REVIEW BEFORE APPLYING

-- Enable RLS on all 11 synced tables
ALTER TABLE public.businesses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.parties ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.invoices ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.invoice_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.expenses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.journal_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.journal_entry_lines ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.khata_entries ENABLE ROW LEVEL SECURITY;

-- Helper RLS Macro Policy Generation for User Scoped Tables
DO $$
DECLARE
    tbl text;
    tables text[] := ARRAY[
        'businesses', 'parties', 'items', 'invoices', 'invoice_items',
        'payments', 'expenses', 'accounts', 'journal_entries',
        'journal_entry_lines', 'khata_entries'
    ];
BEGIN
    FOREACH tbl IN ARRAY tables LOOP
        EXECUTE format('DROP POLICY IF EXISTS %I ON public.%I;', tbl || '_user_isolation', tbl);
        EXECUTE format(
            'CREATE POLICY %I ON public.%I FOR ALL USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid());',
            tbl || '_user_isolation', tbl
        );
    END LOOP;
END $$;
```

---

## 3. Realtime & Indexes Setup

```sql
-- REVIEW BEFORE APPLYING

-- Optimize high-frequency sync query filters
CREATE INDEX IF NOT EXISTS idx_businesses_user_id ON public.businesses(user_id);
CREATE INDEX IF NOT EXISTS idx_parties_user_biz ON public.parties(user_id, business_id);
CREATE INDEX IF NOT EXISTS idx_items_user_biz ON public.items(user_id, business_id);
CREATE INDEX IF NOT EXISTS idx_invoices_user_biz ON public.invoices(user_id, business_id);
CREATE INDEX IF NOT EXISTS idx_payments_user_biz ON public.payments(user_id, business_id);
CREATE INDEX IF NOT EXISTS idx_expenses_user_biz ON public.expenses(user_id, business_id);
CREATE INDEX IF NOT EXISTS idx_accounts_user_biz ON public.accounts(user_id, business_id);
CREATE INDEX IF NOT EXISTS idx_khata_user_biz ON public.khata_entries(user_id, business_id);
```
