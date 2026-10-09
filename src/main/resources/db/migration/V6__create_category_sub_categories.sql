CREATE TABLE category_sub_categories (
    category_id     UUID NOT NULL REFERENCES categories (id),
    sub_category_id UUID NOT NULL REFERENCES sub_categories (id),
    PRIMARY KEY (category_id, sub_category_id)
);

CREATE INDEX idx_csc_sub_category ON category_sub_categories (sub_category_id);
