# Handler

https://cloud.tencent.com/developer/article/1800399

https://cloud.tencent.com/developer/article/1924870?from=article.detail.1800399

https://cloud.tencent.com/developer/article/1834340?from=article.detail.1924870

https://cloud.tencent.com/developer/article/1604727?from=article.detail.1924870

https://www.icode9.com/content-4-944093.html

```java
@Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_test1:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mTvTest.setText("子线程真的可以更新UI吗？");
                    }
                }).start();
                break;
            case R.id.btn_test2:   //通过发送消息
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mHandler.sendEmptyMessage(100);
                    }
                }).start();
                break;
            case R.id.btn_test3:  //通过Handler.post方法
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mTvTest.setText("handler.post");
                            }
                        });
                    }
                }).start();
                break;
            case R.id.btn_test4:  //通过 view.post方法
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mTvTest.post(new Runnable() {
                            @Override
                            public void run() {
                                mTvTest.setText("view.post");
                            }
                        });
                    }
                }).start();
                break;
            case R.id.btn_test5:  //通过 activity 的 runOnUiThread方法
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mTvTest.setText("runOnUIThread");
                            }
                        });
                    }
                }).start();
                break;
            default:
                break;
        }
    }
```

