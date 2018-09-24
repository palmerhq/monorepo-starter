import pytest
from flask import url_for


def test_homepage_response(client):
    res = client.get(url_for('homepage'))
    assert res.status_code == 200
    assert res.data == b"Hello, world!"
