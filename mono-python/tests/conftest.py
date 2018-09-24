#!/usr/bin/env python

import pytest

from helloworld import make_app

print(__name__)


@pytest.fixture
def app():
    app_ = make_app()
    return app_
