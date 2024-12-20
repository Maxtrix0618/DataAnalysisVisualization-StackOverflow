CREATE TABLE question (
    question_id BIGINT PRIMARY KEY,
    title TEXT,
    body TEXT,
    score INT,
    tags TEXT,
    owner_user_id BIGINT,
    owner_display_name TEXT,
    owner_reputation INT,
    owner_accept_rate INT,
    view_count INT,
    answer_count INT,
    is_answered BOOLEAN,
    accepted_answer_id BIGINT,
    creation_date TIMESTAMP,
    last_edit_date TIMESTAMP,
    last_activity_date TIMESTAMP
);

CREATE TABLE answer (
    answer_id BIGINT PRIMARY KEY,
    question_id BIGINT,
    body TEXT,
    score INT,
    owner_user_id BIGINT,
    owner_display_name TEXT,
    owner_reputation INT,
    owner_accept_rate INT,
    is_accepted BOOLEAN,
    creation_date TIMESTAMP,
    last_edit_date TIMESTAMP,
    last_activity_date TIMESTAMP,
    order_oq INT,
    FOREIGN KEY (question_id) REFERENCES question (question_id)
);

CREATE TABLE comment (
    comment_id BIGINT PRIMARY KEY,
    post_id BIGINT,
    body TEXT,
    score INT,
    owner_user_id BIGINT,
    owner_display_name TEXT,
    owner_reputation INT,
    owner_accept_rate INT,
    creation_date TIMESTAMP,
    post_to INT
);

CREATE TABLE blot (
    id BIGINT PRIMARY KEY,
    name TEXT
);
